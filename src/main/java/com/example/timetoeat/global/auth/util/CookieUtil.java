package com.example.timetoeat.global.auth.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

import java.util.Optional;

public class CookieUtil {
    public static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";
    private static final String COOKIE_DOMAIN = System.getenv().getOrDefault("AUTH_COOKIE_DOMAIN", "");
    private static final String SAME_SITE_DEFAULT = "None";

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
        addCookie(response, name, value, maxAgeSeconds, COOKIE_DOMAIN, SAME_SITE_DEFAULT);
    }

    public static void addCookie(HttpServletResponse response, String name, String value,
                                 int maxAgeSeconds, String domain, String sameSite) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .domain(domain == null || domain.isBlank() ? null : domain)
                .maxAge(maxAgeSeconds)
                .sameSite(sameSite)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public static void deleteCookie(HttpServletRequest request, HttpServletResponse response, String name) {
        ResponseCookie c1 = ResponseCookie.from(name, "")
                .httpOnly(true).secure(true).path("/")
                .domain(COOKIE_DOMAIN.isBlank() ? null : COOKIE_DOMAIN).maxAge(0).sameSite(SAME_SITE_DEFAULT).build();
        response.addHeader(HttpHeaders.SET_COOKIE, c1.toString());

        ResponseCookie c2 = ResponseCookie.from(name, "")
                .httpOnly(true).secure(true).path("/").maxAge(0).sameSite(SAME_SITE_DEFAULT).build();
        response.addHeader(HttpHeaders.SET_COOKIE, c2.toString());
    }

    public static String extractFromCookieHeader(String cookieHeader, String name) {
        if (cookieHeader == null) return null;
        String[] parts = cookieHeader.split(";");
        for (String part : parts) {
            String[] nv = part.trim().split("=", 2);
            if (nv.length == 2 && nv[0].trim().equals(name)) {
                return nv[1].trim();
            }
        }
        return null;
    }
}
