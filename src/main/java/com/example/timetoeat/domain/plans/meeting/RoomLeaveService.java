package com.example.timetoeat.domain.plans.meeting;

import com.example.timetoeat.domain.invitation.adap.out.persistence.InvitationRepository;
import com.example.timetoeat.domain.invitation.domain.InvitationStatus;
import com.example.timetoeat.domain.participation.adap.out.persistence.ParticipationRepository;
import com.example.timetoeat.domain.post.adap.out.persistence.PostRepository;
import com.example.timetoeat.domain.post.application.service.MatchingPostService;
import com.example.timetoeat.domain.post.domain.Status;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 대기실 "매칭 취소하기"(F46/D13). 계약: qa-cloud/contracts/participation-cancel.md
 *
 * 공고: 참여자 본인만 나간다. 마감 뒤 작성자만 남으면 방을 해산하고, 만남 시각 전이면 공고를 다시 모집(OPEN)한다.
 * 초대: 보낸 사람·받은 사람 누구든 취소하면 초대는 CANCELLED, 방은 해산한다.
 *
 * 잠금: 공고/초대 행을 FOR UPDATE로 먼저 잡는다. 신청·마감·삭제와 같은 행에서 직렬화되고,
 * 확정 결과 INSERT는 FK 검사로 같은 부모 행에 공유 잠금을 걸기 때문에 결과 저장과도 직렬화된다.
 * 결과 존재 확인은 잠금 뒤에 한다.
 */
@Service
@RequiredArgsConstructor
public class RoomLeaveService {
    /** 다시 모집할 때 주는 모집 기간. 공고 생성과 같은 2시간이고, 만남 시각을 넘지 않는다. */
    static final Duration REOPEN_WINDOW = Duration.ofHours(2);

    private final PostRepository posts;
    private final ParticipationRepository participations;
    private final MeetingRepository meetings;
    private final InvitationRepository invitations;
    private final ConfirmedInvitationRepository invitationResults;
    private final MatchingPostService socket;
    private final Clock clock;

    public enum Outcome { LEFT, LEFT_AND_REOPENED, LEFT_AND_DISSOLVED }

    @Transactional
    public Outcome leaveAnnouncement(Long viewer, long postId) {
        if (viewer == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        var post = posts.findForUpdate(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "공고를 찾을 수 없습니다."));
        if (post.getMember().getId().equals(viewer))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "작성자는 참여를 취소할 수 없습니다.");
        if (post.getStatus() == Status.COMPLETED || meetings.existsById(postId))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 확정된 밥약입니다.");
        if (participations.deleteByPostAndMember(postId, viewer) == 0)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "참여 중인 공고가 아닙니다.");
        if (post.getStatus() == Status.OPEN) return Outcome.LEFT; // 아직 방이 없다

        boolean alone = participations.findAllByPostId(postId).isEmpty();
        var outcome = Outcome.LEFT;
        if (alone) {
            var now = LocalDateTime.now(clock);
            if (post.getMeetingAt() != null && post.getMeetingAt().isAfter(now)) {
                var until = now.plus(REOPEN_WINDOW);
                post.setStatus(Status.OPEN);
                post.setCloseReason(null); // 다시 모집 중: 마감 사유를 지운다
                post.setExpiredAt(until.isBefore(post.getMeetingAt()) ? until : post.getMeetingAt());
                outcome = Outcome.LEFT_AND_REOPENED;
            } else {
                outcome = Outcome.LEFT_AND_DISSOLVED; // 만남 시각이 지났으면 되살리지 않는다(대기실 목록에서도 빠진다)
            }
        }
        boolean dissolve = alone;
        afterCommit(() -> socket.leaveAnnouncement(postId, viewer, dissolve));
        return outcome;
    }

    /** 수락된 초대 방에서 나간다. 이미 취소된 초대는 204(재전송 안전). */
    @Transactional
    public void cancelInvitation(Long viewer, long invitationId) {
        if (viewer == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        var inv = invitations.findForUpdate(invitationId)
                .filter(i -> i.getInviter().getId().equals(viewer) || i.getInvitee().getId().equals(viewer))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "초대를 찾을 수 없습니다."));
        if (inv.getStatus() == InvitationStatus.CANCELLED) return;
        if (inv.getStatus() != InvitationStatus.APPROVED)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "수락된 초대만 취소할 수 있습니다.");
        if (invitationResults.existsById(invitationId))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 확정된 초대입니다.");
        invitations.cancelApproved(invitationId);
        afterCommit(() -> socket.leaveInvitation(invitationId, viewer));
    }

    private static void afterCommit(Runnable task) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { task.run(); }
        });
    }
}
