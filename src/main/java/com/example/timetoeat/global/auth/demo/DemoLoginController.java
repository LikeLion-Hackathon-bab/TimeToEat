package com.example.timetoeat.global.auth.demo;

import com.example.timetoeat.global.auth.util.CookieUtil;
import com.example.timetoeat.global.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

/**
 * 카카오 계정 없이 시연 회원(김민준·최서연·박지윤·강태민·이소율)으로 로그인한다. 심사·시연용.
 * 꺼져 있으면(기본) 이 컨트롤러가 없어 404. 응답 모양과 쿠키는 OAuth 성공 처리와 같다.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "demo-login", name = "enabled", havingValue = "true")
public class DemoLoginController {
    private final DemoLoginService service;
    private final DemoLoginRateLimiter rateLimiter;

    public record DemoLoginRequest(String secret, Integer account) {}

    public record DemoLoginResponse(String accessToken, boolean signupRequired) {}

    @PostMapping("/demo-login")
    public ResponseEntity<ApiResponse<DemoLoginResponse>> login(@RequestBody(required = false) DemoLoginRequest body,
                                                                HttpServletRequest request,
                                                                HttpServletResponse response) {
        String ip = request.getRemoteAddr();
        Integer account = body == null ? null : body.account();
        if (service.expired()) {
            // 만료 뒤에는 꺼진 것과 같게 보인다.
            return error(HttpStatus.NOT_FOUND, "HTTP_404", "요청한 경로를 찾을 수 없습니다.");
        }
        if (!rateLimiter.tryAcquire(ip)) {
            log.warn("시연 로그인 제한 초과: account={}, ip={}", account, ip);
            return error(HttpStatus.TOO_MANY_REQUESTS, "DEMO_429", "잠시 뒤 다시 시도해 주세요.");
        }
        DemoLoginService.Result r = service.login(body == null ? null : body.secret(), account);
        log.warn("시연 로그인 {}: account={}, ip={}", r.outcome(), account, ip);
        return switch (r.outcome()) {
            case OK -> {
                CookieUtil.deleteCookie(request, response, CookieUtil.REFRESH_TOKEN_COOKIE_NAME);
                CookieUtil.addCookie(response, CookieUtil.REFRESH_TOKEN_COOKIE_NAME, r.tokens().getRefreshToken(),
                        (int) TimeUnit.MILLISECONDS.toSeconds(r.tokens().getRefreshTokenMaxAge()));
                response.setHeader("Cache-Control", "no-store");
                response.addHeader("X-Access-Token", r.tokens().getAccessToken());
                yield ResponseEntity.ok(ApiResponse.success(
                        new DemoLoginResponse(r.tokens().getAccessToken(), r.signupRequired())));
            }
            case EXPIRED -> error(HttpStatus.NOT_FOUND, "HTTP_404", "요청한 경로를 찾을 수 없습니다.");
            case BAD_SECRET -> error(HttpStatus.FORBIDDEN, "DEMO_403", "시연 코드가 맞지 않습니다.");
            case BAD_ACCOUNT -> error(HttpStatus.BAD_REQUEST, "DEMO_400", "시연 계정은 1~5 중 하나입니다.");
            case NOT_DEMO_MEMBER -> error(HttpStatus.FORBIDDEN, "DEMO_403", "시연 계정을 쓸 수 없습니다.");
        };
    }

    private static ResponseEntity<ApiResponse<DemoLoginResponse>> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(ApiResponse.<DemoLoginResponse>builder().code(code).message(message).build());
    }
}
