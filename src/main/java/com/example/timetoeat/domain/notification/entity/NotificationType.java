package com.example.timetoeat.domain.notification.entity;

/**
 * 알림 유형. ALARM 은 기획 문서 §14.2 일반 알림 14행(15종), LOCAL_NEWS 는 §14.3 다섯 분류.
 * 문구·이동 경로·수신자: qa-cloud/contracts/notification-types.md.
 * 값을 추가하면 db/notification.sql 과 db/notification_types.sql 의 ENUM 도 같이 늘린다(ddl-auto=validate).
 */
public enum NotificationType {
    // ALARM (§14.2)
    POST_TAGGED,
    POST_LIKED,
    COMMENT_CREATED,
    REPLY_CREATED,
    ANNOUNCEMENT_JOINED,
    MATCHING_COMPLETED,
    INVITATION_SENT,
    INVITATION_ACCEPTED,
    INVITATION_REJECTED,
    MEETING_COORDINATING,
    MEETING_CONFIRMED,
    MEETING_REMINDER,
    REFERRAL_ACTIVITY,
    CHALLENGE_ACHIEVED,
    COUPON_ISSUED,

    // LOCAL_NEWS (§14.3)
    SCHOOL_NEWS,
    AREA_NEWS,
    COMMERCIAL_NEWS,
    DISCOUNT_NEWS,
    MEAL_NEWS;

    public NotificationCategory category() {
        return switch (this) {
            case SCHOOL_NEWS, AREA_NEWS, COMMERCIAL_NEWS, DISCOUNT_NEWS, MEAL_NEWS -> NotificationCategory.LOCAL_NEWS;
            default -> NotificationCategory.ALARM;
        };
    }
}
