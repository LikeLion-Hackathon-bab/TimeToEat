package com.example.timetoeat.global.auth.api;

import com.example.timetoeat.global.auth.jwt.JwtProvider;
import com.example.timetoeat.global.auth.jwt.JwtService;
import com.example.timetoeat.global.auth.jwt.RefreshRotationGrace;
import com.example.timetoeat.global.auth.service.MemberService;
import com.example.timetoeat.domain.member.repository.RefreshTokenRepository;
import com.example.timetoeat.global.error.exception.CustomExceptionHandler;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** /auth/refresh: 쿠키 없음·위조·다른 키 서명은 모두 401(이전: 쿠키 없음 400 + ERROR, 서명 불일치 500). */
class AuthControllerRefreshTest {
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        JwtProvider provider = new JwtProvider("0123456789abcdef0123456789abcdef0123456789abcdef");
        JwtService svc = new JwtService(provider, mock(RefreshTokenRepository.class), mock(MemberService.class),
                new RefreshRotationGrace(0));
        mvc = MockMvcBuilders.standaloneSetup(new AuthController(svc))
                .setControllerAdvice(new CustomExceptionHandler()).build();
    }

    @Test void noCookieIs401() throws Exception {
        mvc.perform(post("/api/v1/auth/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("REQUEST_011"));
    }

    @Test void garbageCookieIs401() throws Exception {
        mvc.perform(post("/api/v1/auth/refresh").cookie(new Cookie("refreshToken", "not-a-jwt")))
                .andExpect(status().isUnauthorized());
    }

    @Test void tokenSignedWithOtherKeyIs401() throws Exception {
        JwtProvider other = new JwtProvider("ffffffffffffffffffffffffffffffffffffffffffffffff");
        org.springframework.test.util.ReflectionTestUtils.setField(other, "refreshTockenExpiration", 604_800_000);
        org.springframework.test.util.ReflectionTestUtils.setField(other, "accessTockenExpiration", 3_600_000);
        var member = com.example.timetoeat.domain.member.entity.MemberEntity.builder().id(2L).username("b")
                .email("b@test").role(com.example.timetoeat.global.auth.model.Role.ROLE_MEMBER).build();
        String forged = other.issueToken(member, new java.util.Date()).getRefreshToken();
        mvc.perform(post("/api/v1/auth/refresh").cookie(new Cookie("refreshToken", forged)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("REQUEST_010"));
    }
}
