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
    public ApiResponse<TokenDto> refresh( // <-- 반환 타입을 TokenDto로 변경하는 것을 권장
                                          @CookieValue(name = "refreshToken", required = true) String refreshToken,
                                          HttpServletResponse response
    ) {
        TokenDto tokenDto = jwtService.reissue(refreshToken);

        // setRefreshCookie -> addCookie 로 변경
        CookieUtil.addCookie(response, CookieUtil.REFRESH_TOKEN_COOKIE_NAME, tokenDto.getRefreshToken(), tokenDto.getRefreshTokenMaxAge());

        // 클라이언트가 새로 발급된 액세스 토큰을 받아야 하므로 응답에 담아줍니다.
        return ApiResponse.success(tokenDto);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            // deleteCookie를 사용하기 위해 HttpServletRequest를 파라미터로 추가
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        // CookieValue 어노테이션 대신 CookieUtil을 사용해 가져오도록 변경
        String refreshToken = CookieUtil.getCookie(request, CookieUtil.REFRESH_TOKEN_COOKIE_NAME)
                .map(Cookie::getValue)
                .orElse(null);

        if (refreshToken != null) {
            jwtService.logout(refreshToken);
        }

        // deleteRefreshCookie -> deleteCookie 로 변경
        CookieUtil.deleteCookie(request, response, CookieUtil.REFRESH_TOKEN_COOKIE_NAME);

        return ApiResponse.successMessage("로그아웃 성공");
    }

}
