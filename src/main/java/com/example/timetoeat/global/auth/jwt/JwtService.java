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
        return jwtProvider.issueToken(memberEntity, new Date());
    }

    @Transactional
    public TokenDto reissue(String bearerRefreshToken) {
        String refreshToken = getTokenFromBearer(bearerRefreshToken);
        if (!refreshTokenRepository.existsByToken(refreshToken)) {
            throw new CustomException(GlobalErrorCode.INVALID_REFRESH_TOKEN);
        }
        Long memberId = jwtProvider.getId(refreshToken);
        MemberEntity memberEntity = memberService.getById(memberId);
        TokenDto tokenDto = jwtProvider.issueToken(memberEntity, new Date());

        refreshTokenRepository.deleteByMemberId(memberId); //원래 deleteAllByMemberId 였다가 수정
        refreshTokenRepository.save(
                RefreshToken.create(
                        memberId,
                        tokenDto.getRefreshToken(),
                        jwtProvider.getRefreshTokenExpiration(new Date())
                )
        );

        return tokenDto;
    }


    @Transactional
    public void logout(String bearRefreshToken) {
        String refreshToken = this.getTokenFromBearer(bearRefreshToken);
        jwtProvider.validate(bearRefreshToken);
        refreshTokenRepository.deleteAllByMemberId(jwtProvider.getId(refreshToken));
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
