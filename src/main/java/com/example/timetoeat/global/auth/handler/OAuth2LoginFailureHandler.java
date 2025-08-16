package com.example.timetoeat.global.auth.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException {
        log.error("소셜 로그인 실패", exception);
        // ↓↓↓↓↓↓↓↓↓↓ 디버깅을 위해 이 부분을 추가합니다 ↓↓↓↓↓↓↓↓↓↓
        log.error("!!! 소셜 로그인 실패 원인: {}", exception.getMessage());
        log.error("!!! 발생한 예외 타입: {}", exception.getClass().getSimpleName());
        // 전체 예외 스택 트레이스를 강제로 콘솔에 출력합니다.
        exception.printStackTrace();
        // ↑↑↑↑↑↑↑↑↑↑ 디버깅을 위해 이 부분을 추가합니다 ↑↑↑↑↑↑↑↑↑↑
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");

        String errorMessage = "{\"status\": 401, \"message\": \"Social login failed.\"}";

        response.getWriter().write(errorMessage);
        response.getWriter().flush();
    }
}
