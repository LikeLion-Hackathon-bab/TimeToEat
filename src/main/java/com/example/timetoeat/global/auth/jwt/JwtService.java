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

    @Transactional
    public TokenDto doTokenGenerationProcess(CustomOauth2User principal) {
        Long memberId = principal.getMemberId();
        MemberEntity memberEntity = memberService.getById(memberId);

        Date now = new Date();
        TokenDto tokenDto = jwtProvider.issueToken(memberEntity, now);

        refreshTokenRepository.deleteAllByMemberId(memberId);

        refreshTokenRepository.save(
                RefreshToken.create(
                        memberId,
                        tokenDto.getRefreshToken(),
                        jwtProvider.getRefreshTokenExpiration(now)
                )
        );
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

        if (!refreshTokenRepository.existsByToken(refreshToken)) {
            throw new CustomException(GlobalErrorCode.INVALID_REFRESH_TOKEN);
        }

        Long memberId = jwtProvider.getId(refreshToken);
        MemberEntity member = memberService.getById(memberId);

        Date now = new Date();
        TokenDto tokenDto = jwtProvider.issueToken(member, now);

        refreshTokenRepository.deleteAllByMemberId(memberId);
        refreshTokenRepository.save(
                RefreshToken.create(
                        memberId,
                        tokenDto.getRefreshToken(),
                        jwtProvider.getRefreshTokenExpiration(now)
                )
        );

        return tokenDto;
    }


    @Transactional
    public void logout(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) return;

        // 검증
        jwtProvider.validate(refreshToken);

        // (안전장치) refresh 토큰 타입 확인
        String typ = jwtProvider.getPayload(refreshToken).get("typ", String.class);
        if (!"refresh".equals(typ)) {
            throw new CustomException(GlobalErrorCode.INVALID_REFRESH_TOKEN);
        }

        Long memberId = jwtProvider.getId(refreshToken);
        refreshTokenRepository.deleteAllByMemberId(memberId);
    }

    public String getTokenFromBearer(String bearerToken) {
        if(StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            String[] parts = bearerToken.split(" ");
            if (parts.length == 2) {
                return parts[1];
            }
        }
        return null;
    }
}
