package com.example.timetoeat.global.auth.util;

import com.example.timetoeat.global.error.exception.CustomException;
import com.example.timetoeat.global.error.GlobalErrorCode;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.util.Arrays;

public class CookieUtil {
    public static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";

    private CookieUtil() {
        throw new IllegalArgumentException("Utility class");
    }

    public static void addCookie(HttpServletResponse response, String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setMaxAge(maxAge);
        //cookie.setHttpOnly();
    }

    public static void setRefreshCookie(HttpServletResponse response,
                                        String refreshToken,
                                        int maxAge) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, refreshToken);
        cookie.setHttpOnly(true);
        cookie.setMaxAge(maxAge);
        cookie.setPath("/");
        response.addCookie(cookie);
    }

    public static String getRefreshToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        return Arrays.stream(cookies)
                .filter(c->REFRESH_TOKEN_COOKIE_NAME.equals(c.getName()))
                .findFirst()
                .map(Cookie::getValue)
                .orElseThrow(
                        () -> new CustomException(GlobalErrorCode.COOKIE_NOT_FOUND)
                );
    }

    public static void deleteRefreshCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, null);
        cookie.setHttpOnly(true);
        cookie.setMaxAge(0); // 즉시 만료
        cookie.setPath("/");
        response.addCookie(cookie);
    }
}
