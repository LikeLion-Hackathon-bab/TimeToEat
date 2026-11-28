package com.example.timetoeat.domain.referral.service;

import com.example.timetoeat.domain.coupon.service.CouponCommandService;
import com.example.timetoeat.domain.member.entity.MemberEntity;
import com.example.timetoeat.domain.member.exception.MemberErrorCode;
import com.example.timetoeat.domain.member.repository.MemberJpaRepository;
import com.example.timetoeat.domain.referral.dto.response.ReferralCreateResponse;
import com.example.timetoeat.domain.referral.entity.ReferralCode;
import com.example.timetoeat.domain.referral.exception.ReferralErrorCode;
import com.example.timetoeat.domain.referral.repository.ReferralCodeRepository;
import com.example.timetoeat.global.error.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class ReferralCommandService {

    private final ReferralCodeRepository referralRepo;
    private final MemberJpaRepository memberRepo;
    private final CouponCommandService couponCommandService;
    private final Clock clock;

    // 추천 링크 주소. 만료된 babmuckdang.site 기본값을 없앴다(재등록되면 공유 링크가 남의 사이트로 간다).
    // 우선순위: app.referral.base-url → CLIENT_BASE_URL + /invite → CLIENT_LOGIN_REDIRECT 의 origin + /invite → 상대 경로 /invite
    @Value("${app.referral.base-url:}")
    private String configuredBaseUrl;

    @Value("${CLIENT_BASE_URL:}")
    private String clientBaseUrl;

    @Value("${CLIENT_LOGIN_REDIRECT:}")
    private String clientLoginRedirect;

    static String resolveBaseUrl(String configured, String clientBase, String loginRedirect) {
        if (configured != null && !configured.isBlank()) return configured.trim().replaceFirst("/+$", "");
        if (clientBase != null && !clientBase.isBlank()) return clientBase.trim().replaceFirst("/+$", "") + "/invite";
        if (loginRedirect != null && !loginRedirect.isBlank()) {
            try {
                var u = java.net.URI.create(loginRedirect.trim());
                if (u.getScheme() != null && u.getRawAuthority() != null)
                    return u.getScheme() + "://" + u.getRawAuthority() + "/invite";
            } catch (IllegalArgumentException ignore) { /* 아래 상대 경로 */ }
        }
        return "/invite";
    }

    @Value("${app.referral.ttl-days:7}")
    private int ttlDays;

    private static final char[] ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final SecureRandom RND = new SecureRandom();

    private String newCode() {
        while (true) {
            StringBuilder sb = new StringBuilder(10);
            for (int i = 0; i < 10; i++) sb.append(ALPHABET[RND.nextInt(ALPHABET.length)]);
            String code = sb.toString();
            if (!referralRepo.existsByCode(code)) return code;
        }
    }

    // 추천 코드 생성
    public ReferralCreateResponse create(Long meId) {
        MemberEntity me = memberRepo.findById(meId)
                .orElseThrow(() -> new CustomException(MemberErrorCode.MEMBER_NOT_FOUND));

        LocalDateTime now = LocalDateTime.now(clock);
        while (true) {
            String code = newCode();
            try {
                ReferralCode saved = referralRepo.save(
                        ReferralCode.builder()
                                .inviter(me)
                                .code(code)
                                .expiresAt(now.plusDays(ttlDays))
                                .build()
                );
                String link = resolveBaseUrl(configuredBaseUrl, clientBaseUrl, clientLoginRedirect) + "?code=" + saved.getCode();
                return ReferralCreateResponse.of(saved.getCode(), link, saved.getExpiresAt());
            } catch (DataIntegrityViolationException dup) {
                // 같은 시각에 같은 코드가 생성된 케이스 → 재시도
            }
        }
    }

    // 추천 코드 수령(가입 완료 후 로그인 상태에서 호출)
    public void redeem(Long meId, String code) {
        MemberEntity me = memberRepo.findById(meId)
                .orElseThrow(() -> new CustomException(MemberErrorCode.MEMBER_NOT_FOUND));

        String key = code == null ? null : code.trim().toUpperCase();
        var entity = referralRepo.findForUpdateByCode(key)
                .orElseThrow(() -> new CustomException(ReferralErrorCode.CODE_NOT_FOUND));

        LocalDateTime now = LocalDateTime.now(clock);

        if (entity.isExpired(now)) throw new CustomException(ReferralErrorCode.CODE_EXPIRED);
        if (entity.isUsed()) throw new CustomException(ReferralErrorCode.CODE_ALREADY_USED);
        if (entity.getInviter().getId().equals(me.getId()))
            throw new CustomException(ReferralErrorCode.SELF_REFERRAL_NOT_ALLOWED);

        // 회원당 1회: 다른 추천인의 코드를 또 등록해 INVITE 쿠폰을 반복 수령하지 못하게
        if (referralRepo.existsByUsedBy_Id(me.getId()))
            throw new CustomException(ReferralErrorCode.ALREADY_REDEEMED);

        entity.markUsed(me, now);
        try {
            // 서로 다른 코드로 동시에 들어온 두 요청은 위 검사를 함께 통과할 수 있다 → uk_referral_code_used_by 가 막는다.
            // 쿠폰 발급 전에 flush 해 충돌을 여기서 받는다.
            referralRepo.saveAndFlush(entity);
        } catch (DataIntegrityViolationException dup) {
            throw new CustomException(ReferralErrorCode.ALREADY_REDEEMED);
        }
        couponCommandService.issueRewardCoupon(me.getId(), "INVITE");
    }
}
