package com.example.timetoeat.domain.notification.entity;

import com.example.timetoeat.global.util.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 통합 알림(기획 문서 §14.4).
 * - ALARM: receiverId 필수, 읽음·삭제 가능.
 * - LOCAL_NEWS: receiverId 가 null 이면 전체 공개 소식(읽음·삭제 대상 아님).
 * dedupKey: RabbitMQ 재전달·중복 소비 시 같은 알림이 두 번 생기지 않게 하는 키.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "notification",
        indexes = @Index(name = "idx_notification_receiver_category", columnList = "receiver_id, category, id"),
        uniqueConstraints = @UniqueConstraint(name = "uk_notification_dedup_key", columnNames = "dedup_key")
)
public class Notification extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "receiver_id")
    private Long receiverId;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 16)
    private NotificationCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 32)
    private NotificationType type;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "body", nullable = false, length = 500)
    private String body;

    @Column(name = "url", length = 255)
    private String url;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    /** 동네 소식 노출 기간 표시용(예: "8/25 ~ 8/31"). 없으면 null. */
    @Column(name = "period", length = 100)
    private String period;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "dedup_key", length = 120)
    private String dedupKey;

    @Builder
    private Notification(Long receiverId, NotificationType type, String title, String body,
                         String url, String imageUrl, String period, String dedupKey) {
        this.receiverId = receiverId;
        this.type = type;
        this.category = type.category();
        this.title = title;
        this.body = body;
        this.url = url;
        this.imageUrl = imageUrl;
        this.period = period;
        this.dedupKey = dedupKey;
        this.read = false;
    }

    public void markRead(LocalDateTime now) {
        if (!this.read) {
            this.read = true;
            this.readAt = now;
        }
    }
}
