package com.example.timetoeat.global.auth.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.util.SerializationUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Base64;
import java.util.Optional;

public class CookieUtil {
    public static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";
    private static final String COOKIE_DOMAIN = ".babmuckdang.site";
    private static final String SAME_SITE = "Lax";

    public static Optional<Cookie> getCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(name)) {
                    return Optional.of(cookie);
                }
            }
        }
        return Optional.empty();
    }

    public static void addCookie(HttpServletResponse response, String name, String value, int maxAgeSeconds) {
        addCookie(response, name, value, maxAgeSeconds, ".babmuckdang.site", "Lax"); // 기본값 유지
    }

    public static void addCookie(HttpServletResponse response, String name, String value,
                                 int maxAgeSeconds, String domain, String sameSite) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .domain(domain)
                .maxAge(maxAgeSeconds)
                .sameSite(sameSite)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public static void deleteCookie(HttpServletRequest request, HttpServletResponse response, String name) {
        // 도메인 지정 쿠키 삭제
        ResponseCookie c1 = ResponseCookie.from(name, "")
                .httpOnly(true).secure(true).path("/")
                .domain(COOKIE_DOMAIN).maxAge(0).sameSite(SAME_SITE).build();
        response.addHeader(HttpHeaders.SET_COOKIE, c1.toString());

        // 과거에 domain 없이 심긴 쿠키도 함께 삭제
        ResponseCookie c2 = ResponseCookie.from(name, "")
                .httpOnly(true).secure(true).path("/").maxAge(0).sameSite(SAME_SITE).build();
        response.addHeader(HttpHeaders.SET_COOKIE, c2.toString());
    }

    public static String serialize(Object object) {
        return Base64.getUrlEncoder().encodeToString(SerializationUtils.serialize(object));
    }

    public static <T> T deserialize(Cookie cookie, Class<T> cls) {
        byte[] decodedBytes = Base64.getUrlDecoder().decode(cookie.getValue());
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(decodedBytes))) {
            Object object = ois.readObject();
            return cls.cast(object);
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException("Cookie deserialization failed", e);
        }
    }
}
