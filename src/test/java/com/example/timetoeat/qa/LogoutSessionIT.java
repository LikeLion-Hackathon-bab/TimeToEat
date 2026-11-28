package com.example.timetoeat.qa;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 보안 필터 체인 전체를 거친 /api/v1/auth/logout 도 세션을 끝내고 JSESSIONID 를 만료시킨다. */
@AutoConfigureMockMvc
class LogoutSessionIT extends QaIntegrationSupport {
    @Autowired MockMvc mvc;

    @Test
    void logoutEndsOauthSession() throws Exception {
        var session = new MockHttpSession();
        session.setAttribute("org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizationRequestRepository.AUTHORIZATION_REQUEST", "x");
        var res = mvc.perform(post("/api/v1/auth/logout").session(session)).andExpect(status().isOk()).andReturn().getResponse();
        assertTrue(session.isInvalid());
        assertTrue(res.getHeaders("Set-Cookie").stream()
                .anyMatch(c -> c.startsWith("JSESSIONID=;") && c.contains("Path=/") && c.contains("Max-Age=0")),
                res.getHeaders("Set-Cookie").toString());
    }
}
