package com.example.timetoeat.global.auth.dto;

/**
 * POST /api/v1/auth/refresh 응답 본문. refresh 토큰은 HttpOnly 쿠키로만 보낸다(보안 점검 S2).
 * 모양: {"code":"...","data":{"accessToken":"..."}}
 */
public record AccessTokenResponse(String accessToken) {
    public static AccessTokenResponse of(TokenDto tokens) {
        return new AccessTokenResponse(tokens.getAccessToken());
    }
}
