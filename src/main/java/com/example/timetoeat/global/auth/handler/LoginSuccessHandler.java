package com.example.timetoeat.global.auth.handler;

import com.example.timetoeat.global.auth.dto.TokenDto;
import com.example.timetoeat.global.auth.jwt.JwtService;
import com.example.timetoeat.global.auth.model.CustomOauth2User;
import com.example.timetoeat.global.auth.util.CookieUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
@Component
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtService jwtService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        log.info("소셜 로그인 성공 핸들러 동작: req {}", request.toString());
        CustomOauth2User principal = (CustomOauth2User) authentication.getPrincipal();
        log.info("principal : {}", principal);

        TokenDto tokenDto = jwtService.doTokenGenerationProcess(principal);
        log.info("tokenDto : accessToken={}, refreshToken={}", tokenDto.getAccessToken(), tokenDto.getRefreshToken());

        CookieUtil.setRefreshCookie(response, tokenDto.getRefreshToken(), tokenDto.getRefreshTokenMaxAge());

        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_OK);
        String tokenJson = String.format(
                "{\"accessToken\":\"%s\"}",
                tokenDto.getAccessToken()
        );
        response.getWriter().write(tokenJson);
        response.getWriter().flush();
    }
}
