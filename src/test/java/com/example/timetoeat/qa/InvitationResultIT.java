package com.example.timetoeat.qa;

import com.example.timetoeat.domain.invitation.adap.out.persistence.InvitationEntity;
import com.example.timetoeat.domain.invitation.adap.out.persistence.InvitationRepository;
import com.example.timetoeat.domain.invitation.domain.InvitationStatus;
import com.example.timetoeat.domain.member.entity.MemberEntity;
import com.example.timetoeat.domain.participation.adap.out.persistence.ParticipationEntity;
import com.example.timetoeat.domain.participation.adap.out.persistence.ParticipationRepository;
import com.example.timetoeat.domain.post.domain.Status;
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

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 계약 qa-cloud/contracts/invitation-result.md (D23/F62). TZ=UTC 로 실행한다. */
@AutoConfigureMockMvc
class InvitationResultIT extends QaIntegrationSupport {
    @Autowired MockMvc mvc;
    @Autowired InvitationRepository invitations;
    @Autowired ParticipationRepository participations;

    private RequestPostProcessor as(long memberId) {
        var user = new CustomOauth2User(Oauth2UserInfo.fromJwt(memberId, "qa", "qa@qa.test", Role.ROLE_MEMBER));
        return authentication(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    private MemberEntity inviter, invitee;

    private long invitation(InvitationStatus status) {
        inviter = member("보낸사람");
        invitee = member("받은사람");
        return invitations.save(new InvitationEntity(null, inviter, invitee, "밥", status, LocalDateTime.now())).getId();
    }

    private org.springframework.test.web.servlet.ResultActions result(long id, String json, String key) throws Exception {
        var req = post("/api/v1/meetings/invitations/" + id + "/result").contentType(MediaType.APPLICATION_JSON).content(json);
        if (key != null) req.header("X-Room-Key", key);
        return mvc.perform(req);
    }

    private static String body(String meetingAt, String location, String restaurant) {
        return "{\"meetingAt\":\"" + meetingAt + "\",\"location\":\"" + location + "\",\"restaurant\":\"" + restaurant + "\"}";
    }

    @Test
    void resultIsStoredOnceInSeoulWallTimeAndConflictsOnOtherValues() throws Exception {
        long id = invitation(InvitationStatus.APPROVED);
        result(id, body("2026-12-01T13:00:00+09:00", "강남역", "초밥집"), "qa-room-key").andExpect(status().isNoContent());
        // 같은 순간을 UTC로 보낸 재전송도 같은 값
        result(id, body("2026-12-01T04:00:00Z", "강남역", "초밥집"), "qa-room-key").andExpect(status().isNoContent());
        result(id, body("2026-12-01T13:00:00+09:00", "강남역", "국밥집"), "qa-room-key").andExpect(status().isConflict());
        result(id, body("2026-12-01T14:00:00+09:00", "강남역", "초밥집"), "qa-room-key").andExpect(status().isConflict());
        assertEquals(1, jdbc.queryForObject("select count(*) from confirmed_invitation_meeting where invitation_id=?", Integer.class, id));
        assertEquals("2026-12-01 13:00:00", jdbc.queryForObject(
                "select date_format(meeting_at,'%Y-%m-%d %H:%i:%s') from confirmed_invitation_meeting where invitation_id=?", String.class, id));
    }

    @Test
    void errorContract() throws Exception {
        long pending = invitation(InvitationStatus.PENDING);
        String ok = body("2026-12-01T13:00:00+09:00", "강남역", "초밥집");
        result(pending, ok, "qa-room-key").andExpect(status().isConflict());
        result(987_654_321L, ok, "qa-room-key").andExpect(status().isNotFound());
        long id = invitation(InvitationStatus.APPROVED);
        result(id, ok, null).andExpect(status().isBadRequest());
        result(id, ok, "wrong").andExpect(status().isForbidden());
        result(id, body("2026-12-01T13:00:00", "강남역", "초밥집"), "qa-room-key").andExpect(status().isBadRequest());
        result(id, body("2026-12-01T13:00:00+09:00", "강남역", " "), "qa-room-key").andExpect(status().isBadRequest());
        result(id, body("2026-12-01T13:00:00+09:00", "", "초밥집"), "qa-room-key").andExpect(status().isBadRequest());
        result(id, "{\"invitationId\":" + (id + 1) + ",\"meetingAt\":\"2026-12-01T13:00:00+09:00\",\"location\":\"a\",\"restaurant\":\"b\"}",
                "qa-room-key").andExpect(status().isBadRequest());
        assertEquals(0, jdbc.queryForObject("select count(*) from confirmed_invitation_meeting where invitation_id in (?,?)",
                Integer.class, id, pending));
    }

    @Test
    void bothPartiesSeeTheInvitationMealAndOutsidersDoNot() throws Exception {
        long id = invitation(InvitationStatus.APPROVED);
        // 같은 사람에게 공고 밥약도 하나 있다(정렬·type 구분 확인)
        var post = savePost(inviter, 1, Status.CLOSED, LocalDateTime.now(), LocalDateTime.of(2026, 11, 30, 12, 0));
        participations.save(new ParticipationEntity(post, member("공고친구")));
        jdbc.update("insert into confirmed_meeting(post_id, restaurant) values (?, '공고식당')", post.getId());
        result(id, body("2026-12-01T04:00:00Z", "강남역", "초밥집"), "qa-room-key").andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/meetings").with(as(invitee.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(id))
                .andExpect(jsonPath("$.data[0].type").value("INVITATION"))
                .andExpect(jsonPath("$.data[0].detailPath").value("/meetings/invitations/" + id))
                .andExpect(jsonPath("$.data[0].time").value("12월 1일 13:00"))
                .andExpect(jsonPath("$.data[0].location").value("강남역"))
                .andExpect(jsonPath("$.data[0].restaurant").value("초밥집"))
                .andExpect(jsonPath("$.data[0].restaurantType").value(""))
                .andExpect(jsonPath("$.data[0].participants[0].userId").value(inviter.getId()))
                .andExpect(jsonPath("$.data[0].participants[1].userId").value(invitee.getId()));
        mvc.perform(get("/api/v1/meetings").with(as(inviter.getId())))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].type").value("INVITATION"))   // 12/1 13:00 이 11/30 12:00 보다 뒤
                .andExpect(jsonPath("$.data[1].type").value("ANNOUNCEMENT"))
                .andExpect(jsonPath("$.data[1].detailPath").value("/meetings/" + post.getId()));
        mvc.perform(get("/api/v1/meetings/invitations/" + id).with(as(inviter.getId())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.restaurant").value("초밥집"));
        var outsider = member("남");
        mvc.perform(get("/api/v1/meetings/invitations/" + id).with(as(outsider.getId()))).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/meetings").with(as(outsider.getId()))).andExpect(jsonPath("$.data.length()").value(0));
        mvc.perform(get("/api/v1/meetings/invitations/" + id)).andExpect(status().isUnauthorized());
    }

    @Test
    void confirmedInvitationRoomIsClosed() throws Exception {
        long id = invitation(InvitationStatus.APPROVED);
        mvc.perform(get("/api/v1/invitations/accepted").with(as(invitee.getId())))
                .andExpect(jsonPath("$.data[0]").value(id));
        mvc.perform(post("/api/v1/invitations/" + id + "/enter").with(as(invitee.getId()))).andExpect(status().isNoContent());
        verify(socket, times(1)).ensureInvitation(any(), any(), any());

        result(id, body("2026-12-01T13:00:00+09:00", "강남역", "초밥집"), "qa-room-key").andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/invitations/accepted").with(as(invitee.getId())))
                .andExpect(jsonPath("$.data.length()").value(0));
        mvc.perform(get("/api/v1/invitations/accepted").with(as(inviter.getId())))
                .andExpect(jsonPath("$.data.length()").value(0));
        mvc.perform(post("/api/v1/invitations/" + id + "/enter").with(as(inviter.getId())))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.message").value("이미 확정된 초대입니다."));
        verify(socket, times(1)).ensureInvitation(any(), any(), any());
    }
}
