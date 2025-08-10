package com.example.timetoeat.global.auth.api;

import com.example.timetoeat.global.auth.dto.TokenDto;
import com.example.timetoeat.global.auth.jwt.JwtService;
import com.example.timetoeat.global.auth.util.CookieUtil;
import com.example.timetoeat.global.common.ApiResponse;
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
    public ApiResponse<Void> refresh(
            @CookieValue(name = "refreshToken", required = true) String refreshToken,
            HttpServletResponse response
    ) {
        TokenDto tokenDto = jwtService.reissue(refreshToken);
        CookieUtil.setRefreshCookie(response, tokenDto.getRefreshToken(), tokenDto.getRefreshTokenMaxAge());
        return ApiResponse.successMessage("리프레시 성공");
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @CookieValue(name = "refreshToken", required = true) String refreshToken,
            HttpServletResponse response
    )
    {
        jwtService.logout(refreshToken);
        CookieUtil.deleteRefreshCookie(response);
        return ApiResponse.successMessage("로그아웃 성공");
    }
}
