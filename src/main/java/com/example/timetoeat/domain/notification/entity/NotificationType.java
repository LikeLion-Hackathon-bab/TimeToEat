package com.example.timetoeat.domain.notification.entity;

/**
 * 알림 유형. ALARM 은 기획 문서 §14.2 이벤트 중 현재 도메인 이벤트가 있는 것만 생성한다.
 * LOCAL_NEWS 는 §14.3 다섯 분류.
 */
public enum NotificationType {
    // ALARM (§14.2)
    INVITATION_SENT,
    INVITATION_ACCEPTED,
    INVITATION_REJECTED,
    MATCHING_COMPLETED,

    // LOCAL_NEWS (§14.3)
    SCHOOL_NEWS,
    AREA_NEWS,
    COMMERCIAL_NEWS,
    DISCOUNT_NEWS,
    MEAL_NEWS;

    public NotificationCategory category() {
        return switch (this) {
            case INVITATION_SENT, INVITATION_ACCEPTED, INVITATION_REJECTED, MATCHING_COMPLETED ->
                    NotificationCategory.ALARM;
            default -> NotificationCategory.LOCAL_NEWS;
        };
    }
}
