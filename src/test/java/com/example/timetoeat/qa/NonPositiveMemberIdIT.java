package com.example.timetoeat.qa;

import com.example.timetoeat.domain.participation.application.port.in.usecase.ParticipationUseCase;
import com.example.timetoeat.domain.post.domain.vo.member.MemberId;
import com.example.timetoeat.domain.post.domain.vo.post.PostId;
import com.example.timetoeat.global.auth.dto.Oauth2UserInfo;
import com.example.timetoeat.global.auth.model.CustomOauth2User;
import com.example.timetoeat.global.auth.model.Role;
import com.example.timetoeat.global.error.exception.CustomException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 가짜 시연 친구(음수 id)·없는 회원을 태그·초대·참여에 쓰면 FK 위반 500 대신 400(REQUEST_001). */
@AutoConfigureMockMvc
class NonPositiveMemberIdIT extends QaIntegrationSupport {
    @Autowired MockMvc mvc;
    @Autowired ParticipationUseCase participation;

    private RequestPostProcessor as(long memberId) {
        var user = new CustomOauth2User(Oauth2UserInfo.fromJwt(memberId, "qa", "qa@qa.test", Role.ROLE_MEMBER));
        return authentication(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    private static String article(String tagged) {
        return "{\"imageUrl\":\"https://cdn.example.invalid/a.png\",\"mealTime\":\"12:00:00\",\"method\":\"CAMERA\","
                + "\"taggedMemberIds\":[" + tagged + "]}";
    }

    private int articles() {
        return jdbc.queryForObject("select count(*) from article", Integer.class);
    }

    @Test
    void taggingAFakeOrMissingMemberIs400AndStoresNothing() throws Exception {
        var me = member("작성자");
        int before = articles();
        for (String bad : new String[]{"-1", "0", String.valueOf(Long.MAX_VALUE)}) {
            mvc.perform(post("/api/v1/articles").with(as(me.getId())).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON).content(article(bad)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("REQUEST_001"));
        }
        assertEquals(before, articles());

        var friend = member("친구");
        mvc.perform(post("/api/v1/articles").with(as(me.getId())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(article(friend.getId() + "," + me.getId())))
                .andExpect(status().isCreated());
        assertEquals(1, jdbc.queryForObject("select count(*) from article_tag where tagged_member_id=?",
                Integer.class, friend.getId()));
    }

    @Test
    void invitingAFakeMemberIs400() throws Exception {
        var me = member("보내는이");
        mvc.perform(post("/api/v1/invitations/send").with(as(me.getId())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"inviteeId\":{\"id\":-2},\"message\":\"밥\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQUEST_001"));
    }

    @Test
    void participatingWithANonPositiveIdIs400() throws Exception {
        var owner = member("주최");
        var post = openPost(owner, 3);
        var e = assertThrows(CustomException.class, () -> participation.apply(new MemberId(-5L), new PostId(post.getId())));
        assertEquals(400, e.getBaseError().getHttpStatus().value());
        assertEquals(0, participationRows(post.getId()));
        mvc.perform(post("/api/v1/participate/0").with(as(owner.getId())).with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQUEST_001"));
    }
}
