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
        CustomOauth2User principal = (CustomOauth2User) authentication.getPrincipal();
        if (principal.isSignupRequired()) {
            response.sendRedirect("https://bab-muk-dang-client.vercel.app/onboarding");
            return;
        }

        CookieUtil.deleteCookie(request, response, CookieUtil.REFRESH_TOKEN_COOKIE_NAME);

        TokenDto tokenDto = jwtService.doTokenGenerationProcess(principal);

        CookieUtil.addCookie(
                response,
                CookieUtil.REFRESH_TOKEN_COOKIE_NAME,
                tokenDto.getRefreshToken(),
                (int) java.util.concurrent.TimeUnit.MILLISECONDS.toSeconds(tokenDto.getRefreshTokenMaxAge())
        );

        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Pragma", "no-cache");
        response.addHeader("X-Access-Token", tokenDto.getAccessToken());

        response.sendRedirect("https://bab-muk-dang-client.vercel.app/");
    }
}
