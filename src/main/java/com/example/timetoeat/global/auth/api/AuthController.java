package com.example.timetoeat.global.auth.api;

import com.example.timetoeat.global.auth.dto.TokenDto;
import com.example.timetoeat.global.auth.jwt.JwtService;
import com.example.timetoeat.global.auth.util.CookieUtil;
import com.example.timetoeat.global.common.ApiResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("api/v1/auth")
@RestController
@RequiredArgsConstructor
public class AuthController {
    private final JwtService jwtService;

    @PostMapping("/refresh")
    public ApiResponse<TokenDto> refresh(
                                          @CookieValue(name = "refreshToken", required = true) String refreshToken,
                                          HttpServletResponse response
    ) {
        TokenDto tokenDto = jwtService.reissue(refreshToken);
        CookieUtil.addCookie(response, CookieUtil.REFRESH_TOKEN_COOKIE_NAME, tokenDto.getRefreshToken(),(int) java.util.concurrent.TimeUnit.MILLISECONDS.toSeconds(tokenDto.getRefreshTokenMaxAge()));
        return ApiResponse.success(tokenDto);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String refreshToken = CookieUtil.getCookie(request, CookieUtil.REFRESH_TOKEN_COOKIE_NAME)
                .map(Cookie::getValue)
                .orElse(null);

        if (refreshToken != null) {
            jwtService.logout(refreshToken);
        }

        CookieUtil.deleteCookie(request, response, CookieUtil.REFRESH_TOKEN_COOKIE_NAME);

        return ApiResponse.successMessage("로그아웃 성공");
    }

}
