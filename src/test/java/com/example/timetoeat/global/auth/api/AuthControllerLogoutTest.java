package com.example.timetoeat.global.auth.api;

import com.example.timetoeat.domain.member.repository.RefreshTokenRepository;
import com.example.timetoeat.global.auth.jwt.JwtProvider;
import com.example.timetoeat.global.auth.jwt.JwtService;
import com.example.timetoeat.global.auth.jwt.RefreshRotationGrace;
import com.example.timetoeat.global.auth.service.MemberService;
import com.example.timetoeat.global.error.exception.CustomExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 로그아웃은 HTTP 세션을 무효화하고 JSESSIONID 쿠키를 만료시킨다(이전: 로그아웃 뒤에도 JSESSIONID가 남음). */
class AuthControllerLogoutTest {
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        JwtProvider provider = new JwtProvider("0123456789abcdef0123456789abcdef0123456789abcdef");
        JwtService svc = new JwtService(provider, mock(RefreshTokenRepository.class), mock(MemberService.class),
                new RefreshRotationGrace(0));
        mvc = MockMvcBuilders.standaloneSetup(new AuthController(svc))
                .setControllerAdvice(new CustomExceptionHandler()).build();
    }

    private static String sessionCookie(List<String> setCookies) {
        return setCookies.stream().filter(c -> c.startsWith("JSESSIONID=")).findFirst()
                .orElseThrow(() -> new AssertionError("JSESSIONID 삭제 Set-Cookie 없음: " + setCookies));
    }

    @Test
    void logoutInvalidatesSessionAndExpiresJsessionid() throws Exception {
        var session = new MockHttpSession();
        session.setAttribute("SPRING_SECURITY_SAVED_REQUEST", "x");
        var res = mvc.perform(post("/api/v1/auth/logout").session(session))
                .andExpect(status().isOk()).andReturn().getResponse();
        assertTrue(session.isInvalid(), "세션이 무효화되지 않았다");
        String cookie = sessionCookie(res.getHeaders("Set-Cookie"));
        assertTrue(cookie.startsWith("JSESSIONID=;"), cookie);
        assertTrue(cookie.contains("Path=/"), cookie);
        assertTrue(cookie.contains("Max-Age=0"), cookie);
        // refresh 토큰 쿠키 삭제는 그대로
        assertTrue(res.getHeaders("Set-Cookie").stream().anyMatch(c -> c.startsWith("refreshToken=;") && c.contains("Max-Age=0")));
    }

    @Test
    void logoutWithoutSessionDoesNotCreateOneAndStillExpiresCookie() throws Exception {
        var result = mvc.perform(post("/api/v1/auth/logout")).andExpect(status().isOk()).andReturn();
        assertNull(result.getRequest().getSession(false), "로그아웃이 새 세션을 만들었다");
        assertTrue(sessionCookie(result.getResponse().getHeaders("Set-Cookie")).contains("Max-Age=0"));
    }
}
