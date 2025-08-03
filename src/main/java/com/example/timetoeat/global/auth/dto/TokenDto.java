package com.example.timetoeat.global.auth.dto;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TokenDto {
    private String accessToken;
    private String refreshToken;
    private int refreshTokenMaxAge;

    @Builder
    public TokenDto(String accessToken, String refreshToken, int refreshTokenMaxAge) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.refreshTokenMaxAge = refreshTokenMaxAge;
    }
}
