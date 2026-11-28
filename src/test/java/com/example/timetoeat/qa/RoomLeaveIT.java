package com.example.timetoeat.qa;

import com.example.timetoeat.domain.invitation.adap.out.persistence.InvitationEntity;
import com.example.timetoeat.domain.invitation.adap.out.persistence.InvitationRepository;
import com.example.timetoeat.domain.invitation.domain.InvitationStatus;
import com.example.timetoeat.domain.member.entity.MemberEntity;
import com.example.timetoeat.domain.participation.adap.out.persistence.ParticipationEntity;
import com.example.timetoeat.domain.participation.adap.out.persistence.ParticipationRepository;
import com.example.timetoeat.domain.participation.application.port.in.usecase.ParticipationUseCase;
import com.example.timetoeat.domain.plans.meeting.RoomLeaveService;
import com.example.timetoeat.domain.post.adap.out.persistence.PostEntity;
import com.example.timetoeat.domain.post.domain.Status;
import com.example.timetoeat.domain.post.domain.vo.member.MemberId;
import com.example.timetoeat.domain.post.domain.vo.post.PostId;
import com.example.timetoeat.global.auth.dto.Oauth2UserInfo;
import com.example.timetoeat.global.auth.model.CustomOauth2User;
import com.example.timetoeat.global.auth.model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** F46/D13 대기실 나가기. 계약: qa-cloud/contracts/participation-cancel.md */
@AutoConfigureMockMvc
class RoomLeaveIT extends QaIntegrationSupport {
    @Autowired MockMvc mvc;
    @Autowired ParticipationRepository participations;
    @Autowired InvitationRepository invitations;
    @Autowired ParticipationUseCase apply;
    @Autowired RoomLeaveService rooms;
    @Value("${ROOM_RESULT_KEY:}") String roomKey;

    @BeforeEach void resetSocket() { reset(socket); }

    private RequestPostProcessor as(long memberId) {
        var user = new CustomOauth2User(Oauth2UserInfo.fromJwt(memberId, "qa", "qa@qa.test", Role.ROLE_MEMBER));
        return authentication(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    private void join(PostEntity p, MemberEntity... people) {
        for (var m : people) participations.save(new ParticipationEntity(p, m));
    }

    /** meetingAt은 한국 벽시계다(contracts/time-format.md). 테스트 JVM은 TZ=UTC로 돌 수 있다. */
    private static LocalDateTime seoulNow() { return LocalDateTime.now(java.time.ZoneId.of("Asia/Seoul")); }

    private PostEntity reload(PostEntity p) { return posts.findById(p.getId()).orElseThrow(); }

    private PostEntity closed(MemberEntity owner, LocalDateTime meetingAt) {
        return savePost(owner, 3, Status.CLOSED, LocalDateTime.now().minusMinutes(1), meetingAt);
    }

    @Test
    void openPostLeaveRulesAndOwnerOrStrangerAreRefused() throws Exception {
        var owner = member("모집자"); var friend = member("친구"); var stranger = member("남");
        var p = openPost(owner, 3);
        join(p, friend);
        mvc.perform(delete("/api/v1/participate/" + p.getId()).with(as(owner.getId())))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.message").value("작성자는 참여를 취소할 수 없습니다."));
        mvc.perform(delete("/api/v1/participate/" + p.getId()).with(as(stranger.getId()))).andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/participate/" + p.getId()).with(as(friend.getId()))).andExpect(status().isNoContent());
        assertEquals(0, participationRows(p.getId()));
        assertEquals(Status.OPEN, reload(p).getStatus());
        mvc.perform(delete("/api/v1/participate/" + p.getId()).with(as(friend.getId()))).andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/participate/987654321").with(as(friend.getId()))).andExpect(status().isNotFound());
        verify(socket, never()).leaveAnnouncement(anyLong(), anyLong(), anyBoolean());
    }

    @Test
    void leavingAWaitingRoomWithOthersLeftKeepsThePostClosed() throws Exception {
        var owner = member("모집자"); var a = member("가"); var b = member("나");
        var p = closed(owner, seoulNow().plusDays(1));
        join(p, a, b);
        mvc.perform(delete("/api/v1/participate/" + p.getId()).with(as(a.getId()))).andExpect(status().isNoContent());
        assertEquals(Status.CLOSED, reload(p).getStatus());
        assertEquals(1, participationRows(p.getId()));
        verify(socket).leaveAnnouncement(p.getId(), a.getId(), false);
        // 남은 사람의 대기실은 그대로, 나간 사람 목록에서는 빠진다
        mvc.perform(get("/api/v1/meetings/rooms").with(as(b.getId()))).andExpect(jsonPath("$.data[0].id").value(p.getId()));
        mvc.perform(get("/api/v1/meetings/rooms").with(as(a.getId()))).andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void lastParticipantLeavingReopensThePostForTwoHoursAndOthersCanApply() throws Exception {
        var owner = member("모집자"); var a = member("가"); var next = member("새친구");
        var p = closed(owner, seoulNow().plusDays(1));
        join(p, a);
        var before = seoulNow();
        mvc.perform(delete("/api/v1/participate/" + p.getId()).with(as(a.getId()))).andExpect(status().isNoContent());
        var reopened = reload(p);
        assertEquals(Status.OPEN, reopened.getStatus());
        long minutes = ChronoUnit.MINUTES.between(before, reopened.getExpiredAt());
        assertTrue(minutes >= 119 && minutes <= 121, "모집 기간 2시간: " + minutes);
        verify(socket).leaveAnnouncement(p.getId(), a.getId(), true);
        mvc.perform(get("/api/v1/meetings/rooms").with(as(owner.getId()))).andExpect(jsonPath("$.data.length()").value(0));
        assertTrue(apply.apply(new MemberId(next.getId()), new PostId(p.getId())));
        assertEquals(1, participationRows(p.getId()));
    }

    @Test
    void reopenWindowNeverPassesTheMeetingTime() throws Exception {
        var owner = member("모집자"); var a = member("가");
        var meetingAt = seoulNow().plusMinutes(30).truncatedTo(ChronoUnit.MICROS);
        var p = closed(owner, meetingAt);
        join(p, a);
        mvc.perform(delete("/api/v1/participate/" + p.getId()).with(as(a.getId()))).andExpect(status().isNoContent());
        assertEquals(Status.OPEN, reload(p).getStatus());
        assertEquals(meetingAt, reload(p).getExpiredAt());
    }

    @Test
    void afterTheMeetingTimeTheRoomIsDissolvedButThePostIsNotRevived() throws Exception {
        var owner = member("모집자"); var a = member("가");
        var p = closed(owner, seoulNow().minusMinutes(5));
        join(p, a);
        mvc.perform(delete("/api/v1/participate/" + p.getId()).with(as(a.getId()))).andExpect(status().isNoContent());
        assertEquals(Status.CLOSED, reload(p).getStatus());
        verify(socket).leaveAnnouncement(p.getId(), a.getId(), true);
        mvc.perform(get("/api/v1/meetings/rooms").with(as(owner.getId()))).andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void aConfirmedMealCannotBeLeft() throws Exception {
        var owner = member("모집자"); var a = member("가");
        var p = closed(owner, seoulNow().plusDays(1));
        join(p, a);
        jdbc.update("insert into confirmed_meeting(post_id, restaurant) values (?, '초밥집')", p.getId());
        mvc.perform(delete("/api/v1/participate/" + p.getId()).with(as(a.getId())))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.message").value("이미 확정된 밥약입니다."));
        assertEquals(1, participationRows(p.getId()));
    }

    /** 마지막 참여자 나가기와 늦은 결과 저장이 겹쳐도 "모집 중인데 확정됨"이 생기지 않는다. */
    @RepeatedTest(5)
    void lastLeaveRacesWithLateResult() throws Exception {
        var owner = member("모집자"); var a = member("가");
        var p = closed(owner, seoulNow().plusDays(1));
        join(p, a);
        var pool = Executors.newFixedThreadPool(2);
        var gate = new CountDownLatch(1);
        Future<Integer> leave = pool.submit(() -> { gate.await();
            return mvc.perform(delete("/api/v1/participate/" + p.getId()).with(as(a.getId()))).andReturn().getResponse().getStatus(); });
        Future<Integer> result = pool.submit(() -> { gate.await();
            return mvc.perform(post("/api/v1/meetings/" + p.getId() + "/result").header("X-Room-Key", roomKey)
                    .contentType(MediaType.APPLICATION_JSON).content("{\"restaurant\":\"초밥집\"}")).andReturn().getResponse().getStatus(); });
        gate.countDown();
        int l = leave.get(30, TimeUnit.SECONDS), r = result.get(30, TimeUnit.SECONDS);
        pool.shutdown();
        int confirmed = jdbc.queryForObject("select count(*) from confirmed_meeting where post_id=?", Integer.class, p.getId());
        var status = reload(p).getStatus();
        if (confirmed == 1) { assertEquals(204, r); assertEquals(409, l); assertEquals(Status.CLOSED, status); assertEquals(1, participationRows(p.getId())); }
        else { assertEquals(409, r); assertEquals(204, l); assertEquals(Status.OPEN, status); }
    }

    private long invitation(MemberEntity from, MemberEntity to, InvitationStatus s) {
        return invitations.save(new InvitationEntity(null, from, to, "밥", s, LocalDateTime.now())).getId();
    }

    private String statusOf(long id) {
        return jdbc.queryForObject("select status from invitation where id=?", String.class, id);
    }

    @Test
    void eitherSideCanCancelAnAcceptedInvitationAndTheRoomCannotBeReentered() throws Exception {
        var from = member("보낸"); var to = member("받은"); var stranger = member("남");
        long id = invitation(from, to, InvitationStatus.APPROVED);
        mvc.perform(post("/api/v1/invitations/" + id + "/cancel").with(as(stranger.getId()))).andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/invitations/" + id + "/cancel").with(as(to.getId()))).andExpect(status().isNoContent());
        assertEquals("CANCELLED", statusOf(id));
        verify(socket).leaveInvitation(id, to.getId());
        mvc.perform(post("/api/v1/invitations/" + id + "/cancel").with(as(from.getId()))).andExpect(status().isNoContent());
        verify(socket, times(1)).leaveInvitation(anyLong(), anyLong());
        mvc.perform(post("/api/v1/invitations/" + id + "/enter").with(as(from.getId()))).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/invitations/accepted").with(as(from.getId())))
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem(Math.toIntExact(id)))));
        mvc.perform(post("/api/v1/meetings/invitations/" + id + "/result").header("X-Room-Key", roomKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"meetingAt\":\"2026-12-01T13:00:00+09:00\",\"location\":\"강남역\",\"restaurant\":\"초밥집\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void pendingOrConfirmedInvitationsCannotBeCancelled() throws Exception {
        var from = member("보낸"); var to = member("받은");
        long pending = invitation(from, to, InvitationStatus.PENDING);
        mvc.perform(post("/api/v1/invitations/" + pending + "/cancel").with(as(from.getId()))).andExpect(status().isConflict());
        long done = invitation(from, to, InvitationStatus.APPROVED);
        jdbc.update("insert into confirmed_invitation_meeting(invitation_id, meeting_at, location, restaurant) values (?, now(), '강남역', '초밥집')", done);
        mvc.perform(post("/api/v1/invitations/" + done + "/cancel").with(as(to.getId())))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.message").value("이미 확정된 초대입니다."));
        assertEquals("APPROVED", statusOf(done));
        verify(socket, never()).leaveInvitation(anyLong(), anyLong());
    }
}
