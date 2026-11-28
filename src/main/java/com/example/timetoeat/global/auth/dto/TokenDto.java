package com.example.timetoeat.global.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TokenDto {
    private String accessToken;
    // 서버 안에서만 쓴다. 실수로 응답 본문에 TokenDto를 넣어도 refresh 토큰이 나가지 않게 막는다(S2).
    @JsonIgnore private String refreshToken;
    @JsonIgnore private int refreshTokenMaxAge;

    @Builder
    public TokenDto(String accessToken, String refreshToken, int refreshTokenMaxAge) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.refreshTokenMaxAge = refreshTokenMaxAge;
    }
}
