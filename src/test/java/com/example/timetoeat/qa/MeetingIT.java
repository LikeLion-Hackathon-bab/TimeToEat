package com.example.timetoeat.qa;

import com.example.timetoeat.domain.participation.adap.out.persistence.ParticipationEntity;
import com.example.timetoeat.domain.participation.adap.out.persistence.ParticipationRepository;
import com.example.timetoeat.domain.post.domain.Status;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import com.example.timetoeat.global.auth.dto.Oauth2UserInfo;
import com.example.timetoeat.global.auth.model.CustomOauth2User;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class MeetingIT extends QaIntegrationSupport {
    @Autowired MockMvc mvc;
    @Autowired ParticipationRepository participations;

    private org.springframework.test.web.servlet.request.RequestPostProcessor as(long memberId) {
        var user = new CustomOauth2User(Oauth2UserInfo.fromJwt(memberId, "qa", "qa@qa.test",
                com.example.timetoeat.global.auth.model.Role.ROLE_MEMBER));
        return authentication(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    private long closedMeal(LocalDateTime meetingAt, long[] ids) {
        var owner = member("모집자");
        var friend = member("친구");
        var p = savePost(owner, 1, Status.CLOSED, LocalDateTime.now(), meetingAt);
        participations.save(new ParticipationEntity(p, friend));
        ids[0] = owner.getId(); ids[1] = friend.getId();
        return p.getId();
    }

    private void record(long id, String restaurant, int expected) throws Exception {
        mvc.perform(post("/api/v1/meetings/" + id + "/result").header("X-Room-Key", "qa-room-key")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"restaurant\":\"" + restaurant + "\"}"))
                .andExpect(status().is(expected));
    }

    @Test
    void redeliveredResultIsStoredOnceAndOtherRestaurantConflicts() throws Exception {
        long[] ids = new long[2];
        long id = closedMeal(LocalDateTime.now(ZoneId.of("Asia/Seoul")).plusDays(1), ids);
        record(id, "초밥집", 204);
        record(id, "초밥집", 204);
        record(id, "국밥집", 409);
        // 409 본문에 저장된 식당(Socket이 이 값으로 finish 진행)
        mvc.perform(post("/api/v1/meetings/" + id + "/result").header("X-Room-Key", "qa-room-key")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"restaurant\":\"국밥집\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("HTTP_409"))
                .andExpect(jsonPath("$.message").value("이미 다른 식당으로 확정된 공고입니다."))
                .andExpect(jsonPath("$.data.restaurant").value("초밥집"));
        assertEquals(1, jdbc.queryForObject("select count(*) from confirmed_meeting where post_id=?", Integer.class, id));
        assertEquals("초밥집", jdbc.queryForObject("select restaurant from confirmed_meeting where post_id=?", String.class, id));
    }

    @Test
    void resultNeedsTheRoomKey() throws Exception {
        long[] ids = new long[2];
        long id = closedMeal(LocalDateTime.now().plusDays(1), ids);
        mvc.perform(post("/api/v1/meetings/" + id + "/result").header("X-Room-Key", "wrong")
                .contentType(MediaType.APPLICATION_JSON).content("{\"restaurant\":\"x\"}")).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/meetings/" + id + "/result")
                .contentType(MediaType.APPLICATION_JSON).content("{\"restaurant\":\"x\"}")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/meetings/" + id + "/result").header("X-Room-Key", "qa-room-key")
                .contentType(MediaType.APPLICATION_JSON).content("{\"restaurant\":\" \"}")).andExpect(status().isBadRequest());
    }

    @Test
    void resultForUnknownPostIsNotFoundInsteadOfServerError() throws Exception {
        record(987_654_321L, "초밥집", 404);
    }

    @Test
    void outsiderCannotReadSomeoneElsesMeal() throws Exception {
        long[] ids = new long[2];
        long id = closedMeal(LocalDateTime.now().plusDays(1), ids);
        record(id, "초밥집", 204);
        var outsider = member("남");
        mvc.perform(get("/api/v1/meetings/" + id).with(as(outsider.getId()))).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/meetings").with(as(outsider.getId()))).andExpect(jsonPath("$.data.length()").value(0));
        mvc.perform(get("/api/v1/meetings/" + id).with(as(ids[1])))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.restaurant").value("초밥집"))
                .andExpect(jsonPath("$.data.participants.length()").value(2));
        mvc.perform(get("/api/v1/meetings/" + id)).andExpect(status().isUnauthorized());
    }

    /** 서버 JVM 시간대가 UTC여도 isCompleted는 한국 시각 기준이어야 한다(meetingAt은 KST 벽시계). */
    @Test
    void completionUsesSeoulTimeEvenWhenServerRunsInUtc() throws Exception {
        assertEquals("UTC", TimeZone.getDefault().toZoneId().normalized().getId().replace("Etc/", "").replace("Z", "UTC"),
                "이 테스트는 TZ=UTC로 실행한다");
        var seoul = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        long[] a = new long[2], b = new long[2];
        long soon = closedMeal(seoul.plusMinutes(30), a);
        long past = closedMeal(seoul.minusMinutes(1), b);
        record(soon, "a", 204);
        record(past, "b", 204);
        mvc.perform(get("/api/v1/meetings/" + soon).with(as(a[0]))).andExpect(jsonPath("$.data.isCompleted").value(false));
        mvc.perform(get("/api/v1/meetings/" + past).with(as(b[0]))).andExpect(jsonPath("$.data.isCompleted").value(true));
    }
}
