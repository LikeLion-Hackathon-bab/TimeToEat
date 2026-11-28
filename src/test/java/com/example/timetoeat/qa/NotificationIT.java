package com.example.timetoeat.qa;

import com.example.timetoeat.domain.notification.entity.NotificationType;
import com.example.timetoeat.domain.notification.service.NotificationCommandService;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** ai-notify 작업자 spring.patch: 알림 중복 방지(dedupKey), 본인 알림만 읽기·삭제, push 토큰 등록. */
@AutoConfigureMockMvc
class NotificationIT extends QaIntegrationSupport {
    @Autowired MockMvc mvc;
    @Autowired NotificationCommandService notifications;

    private RequestPostProcessor as(long memberId) {
        var user = new CustomOauth2User(Oauth2UserInfo.fromJwt(memberId, "qa", "qa@qa.test", Role.ROLE_MEMBER));
        return authentication(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    @Test
    void redeliveredEventMakesOneNotificationAndOnlyTheReceiverCanTouchIt() throws Exception {
        var me = member("받는사람"); var other = member("남");
        String key = "INVITATION_SENT:qa-" + me.getId();
        notifications.notify(me.getId(), NotificationType.INVITATION_SENT, "식사 초대가 도착했어요", "본문", "/read-invitation/1", key);
        notifications.notify(me.getId(), NotificationType.INVITATION_SENT, "식사 초대가 도착했어요", "본문", "/read-invitation/1", key);
        assertEquals(1, jdbc.queryForObject("select count(*) from notification where dedup_key=?", Integer.class, key));
        long id = jdbc.queryForObject("select id from notification where dedup_key=?", Long.class, key);

        mvc.perform(get("/api/v1/notifications").with(as(me.getId())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].id").value(id))
                .andExpect(jsonPath("$.data[0].read").value(false));
        mvc.perform(get("/api/v1/notifications/unread-count").with(as(me.getId()))).andExpect(jsonPath("$.data.unread").value(1));
        mvc.perform(get("/api/v1/notifications").with(as(other.getId()))).andExpect(jsonPath("$.data.length()").value(0));
        mvc.perform(patch("/api/v1/notifications/" + id + "/read").with(as(other.getId()))).andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/notifications/" + id).with(as(other.getId()))).andExpect(status().isNotFound());
        mvc.perform(patch("/api/v1/notifications/" + id + "/read").with(as(me.getId()))).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/notifications/unread-count").with(as(me.getId()))).andExpect(jsonPath("$.data.unread").value(0));
        mvc.perform(delete("/api/v1/notifications/" + id).with(as(me.getId()))).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/notifications")).andExpect(status().isUnauthorized());
    }

    @Test
    void pushTokenMovesToTheLatestAccount() throws Exception {
        var a = member("가"); var b = member("나");
        String body = "{\"token\":\"tok-" + a.getId() + "\",\"platform\":\"web\"}";
        mvc.perform(post("/api/v1/push/tokens").with(as(a.getId())).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isNoContent());
        mvc.perform(post("/api/v1/push/tokens").with(as(b.getId())).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isNoContent());
        assertEquals(b.getId(), jdbc.queryForObject("select member_id from push_token where token=?", Long.class, "tok-" + a.getId()));
        mvc.perform(post("/api/v1/push/tokens").with(as(a.getId())).contentType(MediaType.APPLICATION_JSON).content("{\"token\":\"\"}")).andExpect(status().isBadRequest());
    }
}
