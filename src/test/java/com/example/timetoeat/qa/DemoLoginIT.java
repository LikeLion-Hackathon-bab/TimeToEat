package com.example.timetoeat.qa;

import com.example.timetoeat.global.auth.demo.DemoAccounts;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 시연 로그인: 실제 보안 필터 체인·DB로 켜짐/꺼짐과 시연 회원 생성, 발급 토큰으로 인증 API 호출. */
@EnabledIfEnvironmentVariable(named = "QA_IT", matches = "1")
class DemoLoginIT {
    static final String SECRET = "demo-secret-for-integration-test-0123456789";

    @Nested
    @AutoConfigureMockMvc
    @TestPropertySource(properties = {"demo-login.enabled=true", "demo-login.secret=" + SECRET})
    class Enabled extends QaIntegrationSupport {
        @Autowired MockMvc mvc;

        @Test void createsFiveDemoMembersAndLogsIn() throws Exception {
            for (int n = 1; n <= 5; n++) {
                var m = members.findByEmail(DemoAccounts.email(n)).orElseThrow();
                assertEquals(DemoAccounts.nickname(n), m.getUsername());
            }
            var res = mvc.perform(post("/api/v1/auth/demo-login").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"secret\":\"" + SECRET + "\",\"account\":1}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.signupRequired").value(true))
                    .andExpect(cookie().httpOnly("refreshToken", true))
                    .andReturn();
            String access = com.jayway.jsonpath.JsonPath.read(res.getResponse().getContentAsString(), "$.data.accessToken");
            mvc.perform(get("/api/v1/members/me/profile").header("Authorization", "Bearer " + access))
                    .andExpect(status().is2xxSuccessful());
            // 발급된 refresh 쿠키로 갱신도 된다(OAuth 로그인과 같은 저장 경로).
            mvc.perform(post("/api/v1/auth/refresh").cookie(java.util.Arrays.stream(res.getResponse().getCookies())
                            .filter(c -> c.getName().equals("refreshToken") && !c.getValue().isEmpty())
                            .reduce((a, b) -> b).orElseThrow()))
                    .andExpect(status().isOk());
        }

        @Test void wrongSecretIs403WithoutAuth() throws Exception {
            mvc.perform(post("/api/v1/auth/demo-login").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"secret\":\"nope\",\"account\":1}"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @AutoConfigureMockMvc
    class DisabledByDefault extends QaIntegrationSupport {
        @Autowired MockMvc mvc;

        @Autowired com.example.timetoeat.global.auth.demo.DemoTokenRevoker revoker;
        @Autowired com.example.timetoeat.domain.member.repository.RefreshTokenRepository refreshTokens;

        @Test void closedDemoRevokesDemoRefreshTokensOnStartup() {
            var demo = members.findByEmail(DemoAccounts.email(2)).orElseGet(() -> members.save(
                    com.example.timetoeat.domain.member.entity.MemberEntity.builder().username(DemoAccounts.nickname(2))
                            .email(DemoAccounts.email(2)).role(com.example.timetoeat.global.auth.model.Role.ROLE_MEMBER).build()));
            var real = member("real");
            var exp = new java.util.Date(System.currentTimeMillis() + 3_600_000);
            refreshTokens.save(com.example.timetoeat.domain.member.entity.RefreshToken.create(demo.getId(), "demo-rt-" + demo.getId(), exp));
            refreshTokens.save(com.example.timetoeat.domain.member.entity.RefreshToken.create(real.getId(), "real-rt-" + real.getId(), exp));
            revoker.run(null);
            assertTrue(refreshTokens.findByMemberId(demo.getId()).isEmpty());
            assertTrue(refreshTokens.findByMemberId(real.getId()).isPresent());
        }

        @Test void endpointIs404AndNoDemoMembersAreCreated() throws Exception {
            mvc.perform(post("/api/v1/auth/demo-login").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"secret\":\"" + SECRET + "\",\"account\":1}"))
                    .andExpect(status().isNotFound());
        }

        @Test void swaggerIsOffByDefault() throws Exception {
            mvc.perform(get("/v3/api-docs")).andExpect(status().isNotFound());
            mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isNotFound());
        }
    }
}
