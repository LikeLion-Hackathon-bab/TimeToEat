package com.example.timetoeat.qa;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 보안 점검 S1: "/api/v1/**" permitAll 제거. 토큰 없으면 401, 열어 둔 경로는 401이 아니다. */
@AutoConfigureMockMvc
class SecurityAllowlistIT extends QaIntegrationSupport {
    @Autowired MockMvc mvc;

    @Test
    void anonymousCannotReadPlansOrProfiles() throws Exception {
        long id = member("열거대상").getId();
        mvc.perform(get("/api/v1/plans")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/plans").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/members/" + id + "/profile/detail")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/members/" + id + "/profile")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/articles/1/comments")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/posts")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/meetings/1")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/meetings/invitations/1")).andExpect(status().isUnauthorized());
    }

    @Test
    void publicAndServerToServerPathsStayOpen() throws Exception {
        // 리프레시: 보안 필터를 통과해 컨트롤러가 답한다. 쿠키가 없으면 INVALID_REFRESH_TOKEN(401, REQUEST_011).
        // 보안 진입점의 401(본문이 JSON 코드 없음)과 코드로 구분한다.
        mvc.perform(post("/api/v1/auth/refresh")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("REQUEST_011"));
        mvc.perform(post("/api/v1/auth/logout")).andExpect(status().isOk());
        // OAuth 시작은 카카오로 리다이렉트
        mvc.perform(get("/oauth2/authorization/kakao")).andExpect(status().is3xxRedirection());
        // Socket 결과 저장은 X-Room-Key로 보호: 키 없으면 400, 틀리면 403 (401 아님)
        mvc.perform(post("/api/v1/meetings/1/result").contentType(MediaType.APPLICATION_JSON).content("{\"restaurant\":\"a\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/meetings/1/result").header("X-Room-Key", "wrong").contentType(MediaType.APPLICATION_JSON)
                .content("{\"restaurant\":\"a\"}")).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/meetings/invitations/1/result").header("X-Room-Key", "wrong").contentType(MediaType.APPLICATION_JSON)
                .content("{\"meetingAt\":\"2026-12-01T13:00:00+09:00\",\"location\":\"a\",\"restaurant\":\"b\"}")).andExpect(status().isForbidden());
        // AI 지원 API는 X-AI-KEY로 보호(403)
        mvc.perform(get("/api/v1/ai/support/users/1/excluded-codes").header("X-AI-KEY", "wrong")).andExpect(status().isForbidden());
        // API 문서는 SWAGGER_ENABLED(기본 false)일 때만. 꺼져 있으면 인증 실패(401)가 아니라 404.
        mvc.perform(get("/v3/api-docs")).andExpect(status().isNotFound());
    }
}
