package com.example.timetoeat.global.auth.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.regex.Pattern;

/**
 * 소셜 로그인 실패(사용자가 카카오 동의 취소, state 불일치, 공급자 오류 등) 시 Client 로그인 화면으로 돌려보낸다.
 * 이전에는 콜백 주소(브라우저 최상위 이동)에 JSON 401을 써서 사용자가 빈 JSON 화면에 갇혔다(N4).
 * 이동 주소는 성공 처리와 같은 CLIENT_LOGIN_REDIRECT 기준의 /login 으로 고정해 열린 리다이렉트가 없게 한다.
 */
@Slf4j
@Component
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

    static final String ERROR_PARAM = "error";
    static final String REASON_PARAM = "reason";
    static final String ERROR_VALUE = "social_login_failed";
    /** OAuth2 오류 코드(RFC 6749 §4.1.2.1 등)는 소문자·밑줄. 그 밖의 값은 주소에 싣지 않는다. */
    private static final Pattern SAFE_CODE = Pattern.compile("[a-z_]{1,64}");

    private final String loginRedirect;

    public OAuth2LoginFailureHandler(
            @Value("${CLIENT_LOGIN_REDIRECT:https://bab-muk-dang-client.vercel.app/}") String loginRedirect) {
        this.loginRedirect = loginRedirect;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        String reason = reasonOf(exception);
        if ("access_denied".equals(reason)) {
            // 사용자가 동의 화면에서 취소한 정상 흐름
            log.info("소셜 로그인 취소: reason={}", reason);
        } else {
            log.warn("소셜 로그인 실패: reason={}, type={}, message={}",
                    reason, exception.getClass().getSimpleName(), exception.getMessage());
        }
        response.setHeader("Cache-Control", "no-store");
        response.sendRedirect(failureUrl(reason));
    }

    String failureUrl(String reason) {
        UriComponentsBuilder b = UriComponentsBuilder.fromUri(URI.create(loginRedirect).resolve("/login"))
                .replaceQuery(null)
                .fragment(null)
                .queryParam(ERROR_PARAM, ERROR_VALUE);
        if (reason != null) b.queryParam(REASON_PARAM, reason);
        return b.build().encode().toUriString();
    }

    static String reasonOf(AuthenticationException exception) {
        if (exception instanceof OAuth2AuthenticationException oauth && oauth.getError() != null) {
            String code = oauth.getError().getErrorCode();
            if (code != null && SAFE_CODE.matcher(code).matches()) return code;
        }
        return null;
    }
}
