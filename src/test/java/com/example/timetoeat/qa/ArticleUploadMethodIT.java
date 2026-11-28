package com.example.timetoeat.qa;

import com.example.timetoeat.global.auth.dto.Oauth2UserInfo;
import com.example.timetoeat.global.auth.model.CustomOauth2User;
import com.example.timetoeat.global.auth.model.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 게시물 작성 요청의 method(CAMERA/ALBUM)를 버리지 않고 저장해 상세·피드 응답에 싣는다. */
@AutoConfigureMockMvc
class ArticleUploadMethodIT extends QaIntegrationSupport {
    @Autowired MockMvc mvc;

    private RequestPostProcessor as(long memberId) {
        var user = new CustomOauth2User(Oauth2UserInfo.fromJwt(memberId, "qa", "qa@qa.test", Role.ROLE_MEMBER));
        return authentication(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    private long create(long me, String method) throws Exception {
        String location = mvc.perform(post("/api/v1/articles").with(as(me)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"imageUrl\":\"https://cdn.example.invalid/a.png\",\"mealTime\":\"12:00:00\",\"method\":\"" + method + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");
        return Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
    }

    @Test
    void methodIsStoredAndReturned() throws Exception {
        var me = member("작성자");
        long album = create(me.getId(), "ALBUM");
        long camera = create(me.getId(), "camera"); // 기존처럼 대소문자 무시

        assertEquals("ALBUM", jdbc.queryForObject("select upload_method from article where id=?", String.class, album));
        assertEquals("CAMERA", jdbc.queryForObject("select upload_method from article where id=?", String.class, camera));

        mvc.perform(get("/api/v1/articles/" + album).with(as(me.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.method").value("ALBUM"));
        mvc.perform(get("/api/v1/articles/by-author/" + me.getId()).with(as(me.getId())))
                .andExpect(jsonPath("$.content[0].method").value("CAMERA"))
                .andExpect(jsonPath("$.content[1].method").value("ALBUM"));
        mvc.perform(get("/api/v1/articles/home").with(as(me.getId())))
                .andExpect(jsonPath("$.content[0].method").value("CAMERA"));

        mvc.perform(post("/api/v1/articles").with(as(me.getId())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"imageUrl\":\"https://cdn.example.invalid/a.png\",\"mealTime\":\"12:00:00\",\"method\":\"SCAN\"}"))
                .andExpect(status().isBadRequest());
    }

    /** 운영(ddl-auto=validate)용 db/article_upload_method.sql 이 Hibernate가 만드는 열과 같은 모양인지. */
    @Test
    void migrationMatchesTheEntityColumn() throws Exception {
        Map<String, Object> col = jdbc.queryForMap("select data_type, character_maximum_length len, is_nullable from information_schema.columns"
                + " where table_schema = database() and table_name = 'article' and column_name = 'upload_method'");
        assertEquals("varchar", col.get("data_type").toString().toLowerCase());
        assertEquals(16L, ((Number) col.get("len")).longValue());
        assertEquals("YES", col.get("is_nullable"));

        String sql = Files.readString(Path.of("db/article_upload_method.sql"));
        assertTrue(sql.contains("ADD COLUMN upload_method VARCHAR(16) NULL"));
        // 열이 이미 있으면 아무것도 하지 않는다(재실행 안전)
        for (String stmt : sql.replaceAll("(?m)^--.*$", "").split(";")) {
            if (!stmt.isBlank()) jdbc.execute(stmt.trim());
        }
        assertEquals(1, jdbc.queryForObject("select count(*) from information_schema.columns where table_schema = database()"
                + " and table_name = 'article' and column_name = 'upload_method'", Integer.class));
    }
}
