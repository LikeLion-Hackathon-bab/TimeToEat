package com.example.timetoeat.domain.notification.service;

import com.example.timetoeat.domain.invitation.domain.Invitation;
import com.example.timetoeat.domain.invitation.domain.InvitationMatchingResult;
import com.example.timetoeat.domain.invitation.domain.InvitationStatus;
import com.example.timetoeat.domain.invitation.domain.event.InvitationAcceptedEvent;
import com.example.timetoeat.domain.invitation.domain.event.InvitationRejectedEvent;
import com.example.timetoeat.domain.invitation.domain.event.InvitationSentEvent;
import com.example.timetoeat.domain.invitation.domain.vo.InvitationId;
import com.example.timetoeat.domain.member.entity.MemberEntity;
import com.example.timetoeat.domain.member.repository.MemberJpaRepository;
import com.example.timetoeat.domain.notification.entity.NotificationType;
import com.example.timetoeat.domain.participation.application.port.out.LoadParticipation;
import com.example.timetoeat.domain.participation.domain.Participation;
import com.example.timetoeat.domain.post.domain.Post;
import com.example.timetoeat.domain.post.domain.Status;
import com.example.timetoeat.domain.post.domain.event.PostClosedEvent;
import com.example.timetoeat.domain.post.domain.event.PostCompletedEvent;
import com.example.timetoeat.domain.post.domain.event.PostExpiredEvent;
import com.example.timetoeat.domain.post.domain.vo.member.MemberId;
import com.example.timetoeat.domain.post.domain.vo.post.PostId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** §14.2 이벤트 → 알림 변환: 수신자·문구·이동 경로·dedupKey, 매칭 완료 전원 알림, 실패 격리. */
class NotificationEventRecorderTest {
    private static final LocalDateTime EXPIRED = LocalDateTime.of(2026, 11, 28, 13, 0);

    private NotificationCommandService notifications;
    private MemberJpaRepository members;
    private LoadParticipation participation;
    private NotificationEventRecorder recorder;

    @BeforeEach
    void setUp() {
        notifications = mock(NotificationCommandService.class);
        members = mock(MemberJpaRepository.class);
        participation = mock(LoadParticipation.class);
        recorder = new NotificationEventRecorder(notifications, members, participation);
        when(members.findById(1L)).thenReturn(Optional.of(MemberEntity.builder().id(1L).username("가람").build()));
        when(members.findById(2L)).thenReturn(Optional.of(MemberEntity.builder().id(2L).username("나래").build()));
    }

    private static Invitation invitation(InvitationStatus status) {
        return Invitation.withId(new InvitationId(7L), new MemberId(1L), new MemberId(2L), "밥 먹자", status, EXPIRED.minusDays(1));
    }

    private static Post post(String location) {
        return Post.withId(PostId.of(9L), new MemberId(1L), 3, Status.CLOSED, EXPIRED.minusHours(2), EXPIRED,
                EXPIRED.plusDays(1), location, "점심", null);
    }

    @Test
    void invitationSentGoesToInviteeWithInviterName() {
        recorder.record(new InvitationSentEvent(invitation(InvitationStatus.PENDING)));
        verify(notifications).notify(2L, NotificationType.INVITATION_SENT, "식사 초대가 도착했어요",
                "가람님이 함께 밥 먹자고 초대했어요.", "/read-invitation/7", "INVITATION_SENT:7");
    }

    @Test
    void acceptedGoesToInviterAndLinksToWaitingRoom() {
        var inv = invitation(InvitationStatus.APPROVED);
        recorder.record(new InvitationAcceptedEvent(new InvitationMatchingResult(inv, inv.invitationId(),
                List.of(new MemberId(1L), new MemberId(2L)))));
        verify(notifications).notify(1L, NotificationType.INVITATION_ACCEPTED, "초대를 수락했어요",
                "나래님이 초대를 수락했어요. 약속을 조율해 보세요.", "/invitation/waiting/7", "INVITATION_ACCEPTED:7");
    }

    @Test
    void rejectedGoesToInviter() {
        recorder.record(new InvitationRejectedEvent(invitation(InvitationStatus.REJECTED)));
        verify(notifications).notify(1L, NotificationType.INVITATION_REJECTED, "초대가 거절됐어요",
                "나래님이 이번 초대는 어렵다고 했어요.", "/matching", "INVITATION_REJECTED:7");
    }

    @Test
    void unknownMemberNameFallsBackToFriend() {
        when(members.findById(1L)).thenReturn(Optional.empty());
        recorder.record(new InvitationSentEvent(invitation(InvitationStatus.PENDING)));
        verify(notifications).notify(eq(2L), any(), any(), eq("친구님이 함께 밥 먹자고 초대했어요."), any(), any());
    }

    @Test
    void closedCompletedExpiredAllNotifyOwnerAndEveryParticipantOnce() {
        Set<MemberId> joined = new LinkedHashSet<>(List.of(new MemberId(2L), new MemberId(3L), new MemberId(1L)));
        when(participation.getParticipationByPostId(PostId.of(9L))).thenReturn(new Participation(PostId.of(9L), joined));
        for (var event : List.of(new PostClosedEvent(post("강남역")), new PostCompletedEvent(post("강남역")), new PostExpiredEvent(post("강남역")))) {
            clearInvocations(notifications);
            recorder.record(event);
            for (long id : new long[]{1L, 2L, 3L})
                verify(notifications).notify(id, NotificationType.MATCHING_COMPLETED, "밥약 매칭이 완료됐어요",
                        "강남역 밥약 대기실에 입장해 약속을 조율해 보세요.", "/announcement/waiting/9",
                        "MATCHING_COMPLETED:9:" + EXPIRED + ":" + id);
            verify(notifications, times(3)).notify(anyLong(), any(), any(), any(), any(), any());
        }
    }

    @Test
    void nullLocationHasNoLeadingSpace() {
        when(participation.getParticipationByPostId(any())).thenReturn(new Participation(PostId.of(9L), Set.of(new MemberId(2L))));
        recorder.record(new PostClosedEvent(post(null)));
        verify(notifications, atLeastOnce()).notify(anyLong(), any(), any(), eq("밥약 대기실에 입장해 약속을 조율해 보세요."), any(), any());
    }

    @Test
    void postWithoutParticipantsNotifiesNobody() {
        when(participation.getParticipationByPostId(any())).thenReturn(Participation.empty(PostId.of(9L)));
        recorder.record(new PostExpiredEvent(post("강남역")));
        verifyNoInteractions(notifications);
    }

    @Test
    void failureIsSwallowedSoTheMessageIsNotRequeuedForever() {
        when(participation.getParticipationByPostId(any())).thenThrow(new IllegalStateException("db down"));
        assertDoesNotThrow(() -> recorder.record(new PostClosedEvent(post("강남역"))));
        doThrow(new RuntimeException("x")).when(notifications).notify(any(), any(), any(), any(), any(), any());
        assertDoesNotThrow(() -> recorder.record(new InvitationSentEvent(invitation(InvitationStatus.PENDING))));
    }
}
