package com.example.timetoeat.global.auth.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * OAuth2 인가 요청을 쿠키에 담는다(세션 없는 서버).
 * - 도메인: CookieUtil 의 AUTH_COOKIE_DOMAIN 을 따른다. 빈 값이면 host-only 쿠키(Vercel 프록시 1st-party 배포).
 * - 서명: 쿠키 값은 Java 직렬화 바이트라, 브라우저가 보낸 값을 그대로 역직렬화하면 임의 객체 역직렬화가 된다.
 *   서버 키(jwt.secret)로 HMAC-SHA256 서명을 붙이고, 서명이 맞을 때만 역직렬화한다.
 */
@Slf4j
@Component
public class CookieOAuth2AuthorizationRequestRepository implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {
    public static final String OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME = "oauth2_auth_request";
    public static final int COOKIE_EXPIRE_SECONDS = 180;

    private final byte[] key;

    public CookieOAuth2AuthorizationRequestRepository(@Value("${jwt.secret}") String secret) {
        this.key = ("oauth2-request:" + secret).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        return CookieUtil.getCookie(request, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME)
                .map(cookie -> verified(cookie.getValue()))
                .map(payload -> CookieUtil.deserialize(new jakarta.servlet.http.Cookie(OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME, payload),
                        OAuth2AuthorizationRequest.class))
                .orElse(null);
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest, HttpServletRequest request, HttpServletResponse response) {
        if (authorizationRequest == null) {
            CookieUtil.deleteCookie(request, response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
            return;
        }
        String payload = CookieUtil.serialize(authorizationRequest);
        CookieUtil.addCookie(response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME,
                payload + "." + sign(payload), COOKIE_EXPIRE_SECONDS);
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request, HttpServletResponse response) {
        OAuth2AuthorizationRequest loaded = loadAuthorizationRequest(request);
        // 콜백이 끝나면 쿠키를 지운다(같은 인가 요청을 180초 안에 다시 쓰지 못하게).
        CookieUtil.deleteCookie(request, response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
        return loaded;
    }

    /** 서명이 맞으면 직렬화 본문을, 아니면 null 을 돌려준다. */
    String verified(String value) {
        int dot = value == null ? -1 : value.lastIndexOf('.');
        if (dot <= 0) return null;
        String payload = value.substring(0, dot);
        byte[] expected = sign(payload).getBytes(StandardCharsets.US_ASCII);
        byte[] actual = value.substring(dot + 1).getBytes(StandardCharsets.US_ASCII);
        if (!MessageDigest.isEqual(expected, actual)) {
            log.warn("oauth2 authorization request cookie signature mismatch");
            return null;
        }
        return payload;
    }

    String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.US_ASCII)));
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
