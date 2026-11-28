package com.example.timetoeat.qa;

import com.example.timetoeat.global.auth.dto.Oauth2UserInfo;
import com.example.timetoeat.global.auth.dto.TokenDto;
import com.example.timetoeat.global.auth.jwt.JwtService;
import com.example.timetoeat.global.auth.model.CustomOauth2User;
import com.example.timetoeat.global.auth.model.Role;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 보안 점검 S2: refresh 토큰은 HttpOnly 쿠키로만. JSON 본문에는 accessToken만. */
@AutoConfigureMockMvc
class RefreshTokenBodyIT extends QaIntegrationSupport {
    @Autowired MockMvc mvc;
    @Autowired JwtService jwt;
    @Autowired ObjectMapper json;

    @Test
    void refreshBodyCarriesOnlyTheAccessTokenAndTheCookieCarriesTheRefreshToken() throws Exception {
        var m = member("갱신");
        TokenDto tokens = jwt.doTokenGenerationProcess(new CustomOauth2User(Oauth2UserInfo.fromJwt(m.getId(), "갱신", m.getEmail(), Role.ROLE_MEMBER)));
        var res = mvc.perform(post("/api/v1/auth/refresh").cookie(new Cookie("refreshToken", tokens.getRefreshToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
                .andExpect(jsonPath("$.data.refreshTokenMaxAge").doesNotExist())
                .andReturn().getResponse();
        String body = res.getContentAsString();
        String cookie = res.getHeaders("Set-Cookie").stream().filter(h -> h.startsWith("refreshToken=") && !h.startsWith("refreshToken=;"))
                .findFirst().orElseThrow();
        String newRefresh = cookie.substring("refreshToken=".length(), cookie.indexOf(';'));
        assertFalse(newRefresh.isBlank());
        assertTrue(cookie.contains("HttpOnly") && cookie.contains("Secure"), cookie);
        assertFalse(body.contains(newRefresh), "본문에 refresh 토큰 값이 없어야 한다");
        assertFalse(body.contains("refreshToken"));
    }

    @Test
    void tokenDtoNeverSerializesTheRefreshToken() throws Exception {
        String s = json.writeValueAsString(TokenDto.builder().accessToken("a").refreshToken("r-secret").refreshTokenMaxAge(10).build());
        assertEquals("{\"accessToken\":\"a\"}", s);
    }
}
