package com.example.timetoeat.domain.notification.service;

import com.example.timetoeat.domain.article.repository.ArticleRepository;
import com.example.timetoeat.domain.article.repository.ArticleTagRepository;
import com.example.timetoeat.domain.member.entity.MemberEntity;
import com.example.timetoeat.domain.member.repository.MemberJpaRepository;
import com.example.timetoeat.domain.notification.entity.NotificationType;
import com.example.timetoeat.domain.notification.event.NotificationEvents.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 기획 문서 §14.2 중 RabbitMQ 이벤트가 없던 11종을 알림으로 만든다(기존 4종은 NotificationEventRecorder).
 * 문구·이동 경로는 qa-cloud/contracts/notification-types.md 계약과 같아야 한다.
 * 모든 메서드는 커밋 뒤 알림 전용 스레드에서 불린다(트랜잭션 없음, 각 저장이 자기 트랜잭션).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationTriggerRecorder {

    static final Duration REMINDER_BEFORE = Duration.ofMinutes(30);
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("M월 d일 HH:mm");
    private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");
    private static final int PREVIEW = 40;

    private final NotificationCommandService notifications;
    private final MemberJpaRepository members;
    private final ArticleTagRepository tags;
    private final ArticleRepository articles;
    private final JdbcTemplate jdbc;
    private final Clock clock;

    // 1. 식사 인증에 태그됨
    public void onArticleCreated(Long articleId, Long authorId) {
        String author = nameOf(authorId);
        for (Long tagged : new LinkedHashSet<>(tags.findTaggedMemberIdsByArticleId(articleId))) {
            if (Objects.equals(tagged, authorId)) continue;
            notifications.notify(tagged, NotificationType.POST_TAGGED,
                    "식사 인증에 태그됐어요",
                    author + "님이 함께한 식사를 올렸어요.",
                    "/post/" + articleId,
                    "POST_TAGGED:" + articleId + ":" + tagged);
        }
    }

    // 2. 좋아요 등록
    public void onLiked(PostLiked e) {
        if (Objects.equals(e.articleAuthorId(), e.likerId())) return;
        notifications.notify(e.articleAuthorId(), NotificationType.POST_LIKED,
                "좋아요를 받았어요",
                nameOf(e.likerId()) + "님이 내 식사 인증을 좋아해요.",
                "/post/" + e.articleId(),
                "POST_LIKED:" + e.articleId() + ":" + e.likerId()); // 껐다 켜도 한 번만
    }

    // 3·4. 댓글 / 대댓글 등록
    public void onComment(CommentCreated e) {
        String who = nameOf(e.commenterId());
        String text = who + "님: " + preview(e.content());
        String url = "/post/" + e.articleId() + "?comment=" + e.commentId();
        boolean reply = e.parentCommentId() != null;
        if (reply && e.parentAuthorId() != null && !Objects.equals(e.parentAuthorId(), e.commenterId())) {
            notifications.notify(e.parentAuthorId(), NotificationType.REPLY_CREATED,
                    "내 댓글에 답글이 달렸어요", text, url, "REPLY_CREATED:" + e.commentId());
        }
        boolean authorAlreadyToldAsParent = reply && Objects.equals(e.parentAuthorId(), e.articleAuthorId());
        if (!Objects.equals(e.articleAuthorId(), e.commenterId()) && !authorAlreadyToldAsParent) {
            notifications.notify(e.articleAuthorId(), NotificationType.COMMENT_CREATED,
                    "새 댓글이 달렸어요", text, url, "COMMENT_CREATED:" + e.commentId());
        }
    }

    // 5. 밥약 공고 참여
    public void onJoined(AnnouncementJoined e) {
        if (Objects.equals(e.ownerId(), e.joinerId())) return;
        notifications.notify(e.ownerId(), NotificationType.ANNOUNCEMENT_JOINED,
                "내 공고에 새 참여자가 있어요",
                nameOf(e.joinerId()) + "님이 " + place(e.location()) + "공고에 참여했어요. ("
                        + e.currentCount() + "/" + e.targetCount() + "명)",
                "/matching?tab=announcement",
                "ANNOUNCEMENT_JOINED:" + e.postId() + ":" + e.expiredAt() + ":" + e.joinerId());
    }

    // 9. 약속 조율 진행: 대기실에 누가 들어오면 같은 방의 다른 사람에게 회차당 한 번
    public void onRoomEntered(RoomEntered e) {
        String who = nameOf(e.enteredBy());
        if (e.kind() == RoomKind.ANNOUNCEMENT) {
            var post = jdbc.queryForList("select member_id, location, expired_at from post where id=?", e.roomId());
            if (post.isEmpty()) return;
            var row = post.get(0);
            String round = String.valueOf(toLocal(row.get("expired_at")));
            for (Long r : announcementMembers(e.roomId(), ((Number) row.get("member_id")).longValue())) {
                if (Objects.equals(r, e.enteredBy())) continue;
                notifications.notify(r, NotificationType.MEETING_COORDINATING,
                        "약속 조율이 시작됐어요",
                        who + "님이 " + place((String) row.get("location")) + "밥약 대기실에 들어왔어요.",
                        "/announcement/waiting/" + e.roomId(),
                        "MEETING_COORDINATING:A:" + e.roomId() + ":" + round + ":" + r);
            }
        } else {
            for (Long r : invitationMembers(e.roomId())) {
                if (Objects.equals(r, e.enteredBy())) continue;
                notifications.notify(r, NotificationType.MEETING_COORDINATING,
                        "약속 조율이 시작됐어요",
                        who + "님이 초대 대기실에 들어왔어요.",
                        "/invitation/waiting/" + e.roomId(),
                        "MEETING_COORDINATING:I:" + e.roomId() + ":" + r);
            }
        }
    }

    // 10. 식사 약속 확정
    public void onConfirmed(MeetingConfirmed e) {
        meeting(e.kind(), e.roomId()).ifPresent(m -> {
            for (Long r : m.receivers()) {
                notifications.notify(r, NotificationType.MEETING_CONFIRMED,
                        "식사 약속이 확정됐어요",
                        m.meetingAt().format(WHEN) + " " + m.restaurant() + "에서 만나요.",
                        m.url(),
                        "MEETING_CONFIRMED:" + m.code() + ":" + e.roomId() + ":" + r);
            }
        });
    }

    // 11. 식사 약속 시간 도래: 만남 30분 전 안쪽(아직 안 지남)의 확정 약속. 스케줄러가 1분마다 부른다.
    public int remindUpcoming() {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime until = now.plus(REMINDER_BEFORE);
        int sent = 0;
        for (Long id : jdbc.queryForList("select cm.post_id from confirmed_meeting cm join post p on p.id = cm.post_id "
                + "where p.meeting_at > ? and p.meeting_at <= ?", Long.class, now, until))
            sent += remind(RoomKind.ANNOUNCEMENT, id);
        for (Long id : jdbc.queryForList("select invitation_id from confirmed_invitation_meeting "
                + "where meeting_at > ? and meeting_at <= ?", Long.class, now, until))
            sent += remind(RoomKind.INVITATION, id);
        return sent;
    }

    private int remind(RoomKind kind, Long id) {
        var m = meeting(kind, id);
        if (m.isEmpty()) return 0;
        var meeting = m.get();
        for (Long r : meeting.receivers()) {
            notifications.notify(r, NotificationType.MEETING_REMINDER,
                    "곧 식사 약속이 있어요",
                    meeting.meetingAt().format(HHMM) + " " + meeting.restaurant() + " 약속이 곧 시작돼요.",
                    meeting.url(),
                    "MEETING_REMINDER:" + meeting.code() + ":" + id + ":" + meeting.meetingAt() + ":" + r);
        }
        return meeting.receivers().size();
    }

    // 12. 추천인 관련 활동
    public void onReferral(ReferralRedeemed e) {
        if (Objects.equals(e.inviterId(), e.redeemerId())) return;
        notifications.notify(e.inviterId(), NotificationType.REFERRAL_ACTIVITY,
                "추천한 친구가 밥먹댕에 왔어요",
                nameOf(e.redeemerId()) + "님이 내 추천 코드로 가입했어요.",
                "/profile",
                "REFERRAL_ACTIVITY:" + e.referralCodeId());
    }

    // 13. 챌린지 달성: 게시글을 올린 뒤 연속 일수가 7의 배수가 된 날, 말일에 그 달을 다 채운 날
    public void onChallengeProgress(Long memberId) {
        LocalDate today = LocalDate.now(clock);
        int streak = 0;
        for (LocalDate d = today; streak <= 400 && postedOn(memberId, d); d = d.minusDays(1)) streak++;
        if (streak >= 7 && streak % 7 == 0) {
            notifications.notify(memberId, NotificationType.CHALLENGE_ACHIEVED,
                    "주간 챌린지를 달성했어요",
                    "7일 연속 식사 인증 완료! 보상 쿠폰을 받아 보세요.",
                    "/challenge",
                    "CHALLENGE_ACHIEVED:WEEK:" + memberId + ":" + today);
        }
        YearMonth ym = YearMonth.from(today);
        if (today.getDayOfMonth() == ym.lengthOfMonth() && streak >= ym.lengthOfMonth()) {
            notifications.notify(memberId, NotificationType.CHALLENGE_ACHIEVED,
                    "월간 챌린지를 달성했어요",
                    "이번 달 매일 식사 인증 완료! 보상 쿠폰을 받아 보세요.",
                    "/challenge",
                    "CHALLENGE_ACHIEVED:MONTH:" + memberId + ":" + ym);
        }
    }

    // 14. 쿠폰 보상 지급
    public void onCoupon(CouponIssued e) {
        notifications.notify(e.memberId(), NotificationType.COUPON_ISSUED,
                "쿠폰이 도착했어요",
                e.title() + " 쿠폰이 쿠폰함에 들어왔어요.",
                "/coupon",
                "COUPON_ISSUED:" + e.memberId() + ":" + e.couponId());
    }

    // ---- helpers

    private record Meeting(String code, LocalDateTime meetingAt, String restaurant, String url, List<Long> receivers) {}

    private Optional<Meeting> meeting(RoomKind kind, Long id) {
        if (kind == RoomKind.ANNOUNCEMENT) {
            var rows = jdbc.queryForList("select p.member_id, p.meeting_at, cm.restaurant from confirmed_meeting cm "
                    + "join post p on p.id = cm.post_id where cm.post_id=?", id);
            if (rows.isEmpty()) return Optional.empty();
            var row = rows.get(0);
            return Optional.of(new Meeting("A", toLocal(row.get("meeting_at")), (String) row.get("restaurant"),
                    "/meeting?type=ANNOUNCEMENT&id=" + id,
                    announcementMembers(id, ((Number) row.get("member_id")).longValue())));
        }
        var rows = jdbc.queryForList("select meeting_at, restaurant from confirmed_invitation_meeting where invitation_id=?", id);
        if (rows.isEmpty()) return Optional.empty();
        var row = rows.get(0);
        return Optional.of(new Meeting("I", toLocal(row.get("meeting_at")), (String) row.get("restaurant"),
                "/meeting?type=INVITATION&id=" + id, invitationMembers(id)));
    }

    private List<Long> announcementMembers(Long postId, Long ownerId) {
        var set = new LinkedHashSet<Long>();
        set.add(ownerId);
        set.addAll(jdbc.queryForList("select member_id from participation where post_id=? order by id", Long.class, postId));
        return List.copyOf(set);
    }

    private List<Long> invitationMembers(Long invitationId) {
        var rows = jdbc.queryForList("select inviter_id, invitee_id from invitation where id=?", invitationId);
        if (rows.isEmpty()) return List.of();
        var row = rows.get(0);
        return List.of(((Number) row.get("inviter_id")).longValue(), ((Number) row.get("invitee_id")).longValue());
    }

    private boolean postedOn(Long memberId, LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        return articles.countByAuthor_IdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(memberId, start, start.plusDays(1)) > 0;
    }

    private static LocalDateTime toLocal(Object v) {
        if (v instanceof LocalDateTime l) return l;
        if (v instanceof Timestamp t) return t.toLocalDateTime();
        return v == null ? null : LocalDateTime.parse(v.toString().replace(' ', 'T'));
    }

    private static String place(String location) {
        return location == null || location.isBlank() ? "" : location + " ";
    }

    private static String preview(String content) {
        if (content == null) return "";
        String flat = content.replaceAll("\\s+", " ").trim();
        return flat.length() <= PREVIEW ? flat : flat.substring(0, PREVIEW) + "…";
    }

    private String nameOf(Long memberId) {
        if (memberId == null) return "친구";
        return members.findById(memberId).map(MemberEntity::getUsername).filter(n -> !n.isBlank()).orElse("친구");
    }
}
