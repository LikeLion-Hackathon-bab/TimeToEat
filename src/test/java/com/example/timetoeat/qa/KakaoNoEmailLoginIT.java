package com.example.timetoeat.qa;

import com.example.timetoeat.domain.member.repository.SocialAccountRepository;
import com.example.timetoeat.global.auth.jwt.JwtService;
import com.example.timetoeat.global.auth.model.CustomOauth2User;
import com.example.timetoeat.global.auth.service.CustomOauthUserService;
import com.jayway.jsonpath.JsonPath;
import com.sun.net.httpserver.HttpServer;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.test.web.servlet.MockMvc;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 새 카카오 앱(account_email "권한 없음")처럼 email 없이 오는 사용자 정보로
 * 로그인(loadUser) → 회원 생성 → JWT 발급 → JWT 필터 인증 → refresh 까지 실제 빈으로 돈다.
 * 카카오 user-info 는 로컬 HTTP 서버 대역이다(외부 호출 없음).
 */
@AutoConfigureMockMvc
class KakaoNoEmailLoginIT extends QaIntegrationSupport {
    @Autowired MockMvc mvc;
    @Autowired CustomOauthUserService oauthUsers;
    @Autowired JwtService jwt;
    @Autowired SocialAccountRepository socialAccounts;

    private HttpServer kakao;
    private final AtomicReference<String> userJson = new AtomicReference<>();

    @BeforeEach void startFakeKakao() throws Exception {
        kakao = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        kakao.createContext("/v2/user/me", ex -> {
            byte[] body = userJson.get().getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("Content-Type", "application/json;charset=UTF-8");
            ex.sendResponseHeaders(200, body.length);
            ex.getResponseBody().write(body);
            ex.close();
        });
        kakao.start();
    }

    @AfterEach void stopFakeKakao() { kakao.stop(0); }

    private CustomOauth2User login(String json) {
        userJson.set(json);
        var registration = ClientRegistration.withRegistrationId("kakao")
                .clientId("fixture").authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("http://127.0.0.1/login/oauth2/code/kakao")
                .authorizationUri("http://127.0.0.1/authorize").tokenUri("http://127.0.0.1/token")
                .userInfoUri("http://127.0.0.1:" + kakao.getAddress().getPort() + "/v2/user/me")
                .userNameAttributeName("id").build();
        var token = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "fixture-token", Instant.now(), Instant.now().plusSeconds(60));
        return (CustomOauth2User) oauthUsers.loadUser(new OAuth2UserRequest(registration, token));
    }

    private String email(long memberId) {
        return jdbc.queryForObject("select email from member where id=?", String.class, memberId);
    }

    @Test
    void loginWithoutEmailIssuesWorkingTokensAndRefreshes() throws Exception {
        long kakaoId = System.nanoTime();
        var first = login("{\"id\":" + kakaoId + ",\"kakao_account\":{\"profile\":{\"nickname\":\"닉\"}}}");
        assertTrue(first.isSignupRequired());
        assertEquals("kakao_" + kakaoId + "@users.noreply.babmuckdang", email(first.getMemberId()));

        var tokens = jwt.doTokenGenerationProcess(first);
        mvc.perform(get("/api/v1/meetings").header("Authorization", "Bearer " + tokens.getAccessToken()))
                .andExpect(status().isOk());

        var refreshed = mvc.perform(post("/api/v1/auth/refresh").cookie(new Cookie("refreshToken", tokens.getRefreshToken())))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String access2 = JsonPath.read(refreshed, "$.data.accessToken");
        mvc.perform(get("/api/v1/meetings").header("Authorization", "Bearer " + access2)).andExpect(status().isOk());

        // 다시 로그인해도 같은 회원(합성 주소가 고정이라 UNIQUE 충돌 없음), 응답 본문에 합성 주소가 나오지 않는다
        var again = login("{\"id\":" + kakaoId + ",\"kakao_account\":{}}");
        assertFalse(again.isSignupRequired());
        assertEquals(first.getMemberId(), again.getMemberId());
        assertFalse(refreshed.contains("users.noreply.babmuckdang"));

        // 다른 사람도 email 없이 가입할 수 있다(UNIQUE)
        var other = login("{\"id\":" + (kakaoId + 1) + "}");
        assertNotEquals(first.getMemberId(), other.getMemberId());
    }

    @Test
    void existingMemberWithRealEmailKeepsItWhenEmailStopsComing() {
        long kakaoId = System.nanoTime();
        String real = "real-" + kakaoId + "@example.com";
        var first = login("{\"id\":" + kakaoId + ",\"kakao_account\":{\"email\":\"" + real + "\",\"profile\":{\"nickname\":\"닉\"}}}");
        assertEquals(real, email(first.getMemberId()));
        var later = login("{\"id\":" + kakaoId + ",\"kakao_account\":{\"email\":null}}");
        assertEquals(first.getMemberId(), later.getMemberId());
        assertEquals(real, email(first.getMemberId()));
        assertEquals(real, socialAccounts.findByProviderId("kakao_" + kakaoId).orElseThrow().getEmail());
    }
}
