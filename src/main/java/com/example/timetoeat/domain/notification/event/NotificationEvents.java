package com.example.timetoeat.domain.notification.event;

import java.time.LocalDateTime;

/**
 * 알림(기획 문서 §14.2)을 만들 계기가 되는 사건들. 발생한 쪽 서비스가 트랜잭션 안에서 publishEvent 하고,
 * {@link com.example.timetoeat.domain.notification.listener.NotificationTriggerListener} 가 커밋 뒤에만 알림으로 바꾼다.
 * 엔티티 대신 id·값만 담는다(커밋 뒤 다른 스레드에서 읽으므로 지연 로딩 프록시를 넘기지 않는다).
 */
public final class NotificationEvents {

    private NotificationEvents() {}

    /** 좋아요가 새로 켜짐(취소·중복 클릭은 발행하지 않음). */
    public record PostLiked(Long articleId, Long articleAuthorId, Long likerId) {}

    /** 댓글·대댓글 저장. parentCommentId 가 있으면 대댓글. */
    public record CommentCreated(Long articleId, Long articleAuthorId, Long commentId, Long commenterId,
                                 Long parentCommentId, Long parentAuthorId, String content) {}

    /** 공고 참여 성공. expiredAt 은 모집 회차 구분용(재모집하면 바뀐다). */
    public record AnnouncementJoined(Long postId, Long ownerId, Long joinerId, String location,
                                     int currentCount, int targetCount, LocalDateTime expiredAt) {}

    public enum RoomKind { ANNOUNCEMENT, INVITATION }

    /** 대기실 입장 성공(Socket 방 준비까지 끝남). */
    public record RoomEntered(RoomKind kind, Long roomId, Long enteredBy) {}

    /** Socket 이 확정 결과를 저장함(같은 값 재전송이어도 발행, 중복은 dedup_key 로 막음). */
    public record MeetingConfirmed(RoomKind kind, Long roomId) {}

    /** 추천 코드 수령. */
    public record ReferralRedeemed(Long referralCodeId, Long inviterId, Long redeemerId) {}

    /** 보상 쿠폰 발급. */
    public record CouponIssued(Long memberId, Long couponId, String title) {}
}
