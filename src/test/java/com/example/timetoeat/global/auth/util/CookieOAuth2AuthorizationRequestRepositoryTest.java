package com.example.timetoeat.global.auth.util;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class CookieOAuth2AuthorizationRequestRepositoryTest {
    private final CookieOAuth2AuthorizationRequestRepository repo = new CookieOAuth2AuthorizationRequestRepository("qa-secret");

    private OAuth2AuthorizationRequest request() {
        return OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri("https://kauth.example.invalid/oauth/authorize").clientId("x")
                .redirectUri("https://app.example.invalid/login/oauth2/code/kakao").state("st-1").build();
    }

    private String savedValue() {
        var res = new MockHttpServletResponse();
        repo.saveAuthorizationRequest(request(), new MockHttpServletRequest(), res);
        String header = res.getHeader("Set-Cookie");
        assertTrue(header.contains("Secure") && header.contains("HttpOnly") && header.contains("SameSite=None"), header);
        return header.substring(header.indexOf('=') + 1, header.indexOf(';'));
    }

    private MockHttpServletRequest with(String value) {
        var req = new MockHttpServletRequest();
        req.setCookies(new Cookie(CookieOAuth2AuthorizationRequestRepository.OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME, value));
        return req;
    }

    @Test void signedCookieRoundTrips() {
        var loaded = repo.loadAuthorizationRequest(with(savedValue()));
        assertNotNull(loaded);
        assertEquals("st-1", loaded.getState());
    }

    @Test void unsignedOrTamperedCookieIsNeverDeserialized() {
        String value = savedValue();
        String payload = value.substring(0, value.lastIndexOf('.'));
        assertNull(repo.loadAuthorizationRequest(with(payload)), "서명 없는 옛 형식");
        assertNull(repo.loadAuthorizationRequest(with(payload + ".AAAA")), "틀린 서명");
        // 다른 객체를 직렬화해 붙인 값(역직렬화 공격 모양)도 서명이 없으면 읽지 않는다
        String evil = Base64.getUrlEncoder().encodeToString(org.springframework.util.SerializationUtils.serialize(new java.util.HashMap<>()));
        assertNull(repo.loadAuthorizationRequest(with(evil + "." + "x")));
        // 다른 키로 서명한 값
        String other = new CookieOAuth2AuthorizationRequestRepository("other").sign(payload);
        assertNull(repo.loadAuthorizationRequest(with(payload + "." + other)));
    }

    @Test void removeDeletesTheCookie() {
        var res = new MockHttpServletResponse();
        assertNotNull(repo.removeAuthorizationRequest(with(savedValue()), res));
        assertTrue(res.getHeaders("Set-Cookie").stream().allMatch(h -> h.contains("Max-Age=0")));
    }
}
