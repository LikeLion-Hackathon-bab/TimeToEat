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

    @Value("${app.referral.base-url:https://babmuckdang.site/invite}")
    private String baseUrl;

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
                String link = baseUrl + "?code=" + saved.getCode();
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

        entity.markUsed(me, now);
        couponCommandService.issueRewardCoupon(me.getId(), "INVITE");
    }
}
