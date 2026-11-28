package com.example.timetoeat.domain.notification.service;

import com.example.timetoeat.domain.invitation.domain.Invitation;
import com.example.timetoeat.domain.invitation.domain.event.InvitationAcceptedEvent;
import com.example.timetoeat.domain.invitation.domain.event.InvitationRejectedEvent;
import com.example.timetoeat.domain.invitation.domain.event.InvitationSentEvent;
import com.example.timetoeat.domain.member.entity.MemberEntity;
import com.example.timetoeat.domain.member.repository.MemberJpaRepository;
import com.example.timetoeat.domain.notification.entity.NotificationType;
import com.example.timetoeat.domain.participation.application.port.out.LoadParticipation;
import com.example.timetoeat.domain.post.domain.Post;
import com.example.timetoeat.domain.post.domain.event.PostClosedEvent;
import com.example.timetoeat.domain.post.domain.event.PostCompletedEvent;
import com.example.timetoeat.domain.post.domain.event.PostExpiredEvent;
import com.example.timetoeat.domain.post.domain.vo.member.MemberId;
import com.example.timetoeat.global.util.DomainEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * notification-queue 로 들어온 도메인 이벤트를 알림(§14.2)으로 바꾼다.
 * 현재 이벤트가 있는 4종만 다룬다: 초대 발송 / 초대 수락 / 초대 거절 / 밥약 매칭 완료(마감·인원 충족·만료).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventRecorder {

    private final NotificationCommandService notifications;
    private final MemberJpaRepository members;
    private final LoadParticipation loadParticipation;

    public void record(DomainEvent event) {
        try {
            if (event instanceof InvitationSentEvent e) onInvitationSent(e.invitation());
            else if (event instanceof InvitationAcceptedEvent e) onInvitationAccepted(e.invitationMatchingResult().invitation());
            else if (event instanceof InvitationRejectedEvent e) onInvitationRejected(e.invitation());
            else if (event instanceof PostClosedEvent e) onMatched(e.post());
            else if (event instanceof PostCompletedEvent e) onMatched(e.post());
            else if (event instanceof PostExpiredEvent e) onMatched(e.post());
        } catch (RuntimeException ex) {
            // 알림 실패가 메시지 재전달 루프를 만들지 않도록 기록만 한다.
            log.warn("notification record failed event={} msg={}", event.getClass().getSimpleName(), ex.getMessage());
        }
    }

    private void onInvitationSent(Invitation inv) {
        Long id = inv.invitationId().id();
        notifications.notify(inv.inviteeId().getId(), NotificationType.INVITATION_SENT,
                "식사 초대가 도착했어요",
                nameOf(inv.inviterId()) + "님이 함께 밥 먹자고 초대했어요.",
                "/read-invitation/" + id,
                "INVITATION_SENT:" + id);
    }

    private void onInvitationAccepted(Invitation inv) {
        Long id = inv.invitationId().id();
        notifications.notify(inv.inviterId().getId(), NotificationType.INVITATION_ACCEPTED,
                "초대를 수락했어요",
                nameOf(inv.inviteeId()) + "님이 초대를 수락했어요. 약속을 조율해 보세요.",
                "/invitation/waiting/" + id,
                "INVITATION_ACCEPTED:" + id);
    }

    private void onInvitationRejected(Invitation inv) {
        Long id = inv.invitationId().id();
        notifications.notify(inv.inviterId().getId(), NotificationType.INVITATION_REJECTED,
                "초대가 거절됐어요",
                nameOf(inv.inviteeId()) + "님이 이번 초대는 어렵다고 했어요.",
                "/matching",
                "INVITATION_REJECTED:" + id);
    }

    private void onMatched(Post post) {
        Long postId = post.getPostId().getId();
        Set<MemberId> receivers = new LinkedHashSet<>(loadParticipation.getParticipationByPostId(post.getPostId()).getMemberIds());
        if (receivers.isEmpty()) return; // 참여자 없는 만료 공고는 알리지 않는다
        receivers.add(post.getMemberId());
        String place = post.getLocation() == null ? "" : post.getLocation() + " ";
        for (MemberId receiver : receivers) {
            notifications.notify(receiver.getId(), NotificationType.MATCHING_COMPLETED,
                    "밥약 매칭이 완료됐어요",
                    place + "밥약 대기실에 입장해 약속을 조율해 보세요.",
                    "/announcement/waiting/" + postId,
                    "MATCHING_COMPLETED:" + postId + ":" + receiver.getId());
        }
    }

    private String nameOf(MemberId memberId) {
        return members.findById(memberId.getId()).map(MemberEntity::getUsername).orElse("친구");
    }
}
