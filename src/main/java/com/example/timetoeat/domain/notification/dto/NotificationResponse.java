package com.example.timetoeat.domain.notification.dto;

import com.example.timetoeat.domain.notification.entity.Notification;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        String category,
        String type,
        String title,
        String body,
        String url,
        String imageUrl,
        String period,
        boolean read,
        LocalDateTime createdAt
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId(), n.getCategory().name(), n.getType().name(), n.getTitle(), n.getBody(),
                n.getUrl(), n.getImageUrl(), n.getPeriod(), n.isRead(), n.getCreatedAt());
    }
}
