package com.example.timetoeat.global.auth.model.provider;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class KakaoUserTest {
    private static KakaoUser user(Map<String, Object> attrs) { return new KakaoUser(attrs, "kakao"); }

    private static Map<String, Object> account(Object email) {
        var account = new HashMap<String, Object>();
        if (email != null) account.put("email", email);
        account.put("profile", Map.of("nickname", "닉네임", "profile_image_url", "https://k.example.invalid/p.png"));
        return account;
    }

    @Test void presentEmailIsKept() {
        var u = user(Map.of("id", 1670764001L, "kakao_account", account("real@example.com")));
        assertEquals("real@example.com", u.getEmail());
        assertFalse(KakaoUser.isSyntheticEmail(u.getEmail()));
        assertEquals("닉네임", u.getUsername());
        assertEquals("https://k.example.invalid/p.png", u.getProfileUrl());
        assertEquals("kakao_1670764001", u.getProviderId());
    }

    @Test void nullOrBlankEmailBecomesAStableSyntheticAddress() {
        var missing = user(Map.of("id", 42L, "kakao_account", account(null)));
        assertEquals("kakao_42@users.noreply.babmuckdang", missing.getEmail());
        assertEquals(missing.getEmail(), user(Map.of("id", 42L, "kakao_account", account(null))).getEmail(), "같은 사람은 같은 값");
        assertNotEquals(missing.getEmail(), user(Map.of("id", 43L, "kakao_account", account(null))).getEmail(), "사람마다 다른 값");
        assertTrue(KakaoUser.isSyntheticEmail(missing.getEmail()));

        var attrs = new HashMap<String, Object>(Map.of("id", 42L));
        var acc = account(null); acc.put("email", null); attrs.put("kakao_account", acc);
        assertEquals("kakao_42@users.noreply.babmuckdang", user(attrs).getEmail(), "email: null");
        assertEquals("kakao_42@users.noreply.babmuckdang", user(Map.of("id", 42L, "kakao_account", account("  "))).getEmail(), "빈 문자열");
    }

    @Test void missingKakaoAccountDoesNotThrow() {
        var u = user(Map.of("id", 7L));
        assertEquals("kakao_7@users.noreply.babmuckdang", u.getEmail());
        assertEquals(KakaoUser.DEFAULT_NAME, u.getUsername());
        assertNull(u.getProfileUrl());

        var notAMap = user(Map.of("id", 7L, "kakao_account", "권한 없음"));
        assertEquals("kakao_7@users.noreply.babmuckdang", notAMap.getEmail());
        assertEquals(KakaoUser.DEFAULT_NAME, notAMap.getUsername());
    }

    @Test void missingProfileFallsBackToLegacyPropertiesThenDefault() {
        var legacy = user(Map.of("id", 8L, "kakao_account", Map.of(),
                "properties", Map.of("nickname", "옛닉", "profile_image", "https://k.example.invalid/old.png")));
        assertEquals("옛닉", legacy.getUsername());
        assertEquals("https://k.example.invalid/old.png", legacy.getProfileUrl());
        var noNickname = user(Map.of("id", 9L, "kakao_account", Map.of("profile", Map.of("profile_image_url", "https://k.example.invalid/p.png"))));
        assertEquals(KakaoUser.DEFAULT_NAME, noNickname.getUsername());
        assertEquals("https://k.example.invalid/p.png", noNickname.getProfileUrl());
    }

    @Test void missingKakaoIdIsAnAuthenticationErrorNotASharedAddress() {
        assertThrows(OAuth2AuthenticationException.class, () -> user(Map.of("kakao_account", Map.of())).getEmail());
    }
}
