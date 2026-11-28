package com.example.timetoeat.global.auth.jwt;

import com.example.timetoeat.global.auth.dto.TokenDto;
import com.example.timetoeat.domain.member.entity.MemberEntity;
import com.example.timetoeat.domain.member.entity.RefreshToken;
import com.example.timetoeat.global.auth.model.CustomOauth2User;
import com.example.timetoeat.domain.member.repository.RefreshTokenRepository;
import com.example.timetoeat.global.auth.service.MemberService;
import com.example.timetoeat.global.error.exception.CustomException;
import com.example.timetoeat.global.error.GlobalErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;

import static com.example.timetoeat.global.auth.jwt.JwtProvider.BEARER_PREFIX;

@Slf4j
@RequiredArgsConstructor
@Service
public class JwtService {
    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final MemberService memberService;
    private final RefreshRotationGrace rotationGrace;

    @Transactional
    public TokenDto doTokenGenerationProcess(CustomOauth2User principal) {
        Long memberId = principal.getMemberId();
        MemberEntity memberEntity = memberService.getById(memberId);

        Date now = new Date();
        TokenDto tokenDto = jwtProvider.issueToken(memberEntity, now);
        Date refreshExp = jwtProvider.getRefreshTokenExpiration(now);

        upsertRefreshToken(memberId, tokenDto.getRefreshToken(), refreshExp);
        rotationGrace.forget(memberId); // 새 로그인 뒤에는 이전 회전 유예를 남기지 않는다

        return tokenDto;
    }

    @Transactional
    public TokenDto reissue(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            throw new CustomException(GlobalErrorCode.INVALID_REFRESH_TOKEN);
        }

        jwtProvider.validate(refreshToken);

        String typ = jwtProvider.getPayload(refreshToken).get("typ", String.class);
        if (!"refresh".equals(typ)) {
            throw new CustomException(GlobalErrorCode.INVALID_REFRESH_TOKEN);
        }

        Long memberId = jwtProvider.getId(refreshToken);
        // 같은 회원의 갱신을 행 잠금으로 직렬화한 뒤 토큰을 비교한다(검사와 회전 사이 경합 제거).
        RefreshToken stored = refreshTokenRepository.findByMemberIdForUpdate(memberId)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.INVALID_REFRESH_TOKEN));
        Date now = new Date();
        boolean current = refreshToken.equals(stored.getToken());
        if (!current && !rotationGrace.isRecentlyRotated(memberId, refreshToken, now.getTime())) {
            throw new CustomException(GlobalErrorCode.INVALID_REFRESH_TOKEN);
        }

        MemberEntity member = memberService.getById(memberId);
        TokenDto tokenDto = jwtProvider.issueToken(member, now);
        Date refreshExp = jwtProvider.getRefreshTokenExpiration(now);

        String previous = stored.getToken();
        stored.refresh(tokenDto.getRefreshToken(), refreshExp);
        if (!previous.equals(tokenDto.getRefreshToken())) {
            rotationGrace.remember(memberId, previous, now.getTime());
        }

        return tokenDto;
    }

    @Transactional
    public void logout(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) return;

        jwtProvider.validate(refreshToken);

        String typ = jwtProvider.getPayload(refreshToken).get("typ", String.class);
        if (!"refresh".equals(typ)) {
            throw new CustomException(GlobalErrorCode.INVALID_REFRESH_TOKEN);
        }

        Long memberId = jwtProvider.getId(refreshToken);
        refreshTokenRepository.deleteAllByMemberId(memberId);
        rotationGrace.forget(memberId);
    }

    public String getTokenFromBearer(String bearerToken) {
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            String[] parts = bearerToken.split(" ");
            if (parts.length == 2) {
                return parts[1];
            }
        }
        return null;
    }

    private void upsertRefreshToken(Long memberId, String newToken, Date newExpiredAt) {
        var locked = refreshTokenRepository.findByMemberIdForUpdate(memberId);
        if (locked.isPresent()) {
            locked.get().refresh(newToken, newExpiredAt);
            return;
        }

        try {
            refreshTokenRepository.save(RefreshToken.create(memberId, newToken, newExpiredAt));
        } catch (DataIntegrityViolationException e) {
            log.warn("UPSERT race detected for memberId={}, falling back to update.", memberId);
            refreshTokenRepository.findByMemberIdForUpdate(memberId)
                    .ifPresentOrElse(
                            exist -> exist.refresh(newToken, newExpiredAt),
                            () -> { throw e; }
                    );
        }
    }
}
