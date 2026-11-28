package com.example.timetoeat.domain.notification.controller;

import com.example.timetoeat.domain.notification.dto.NotificationResponse;
import com.example.timetoeat.domain.notification.dto.UnreadCountResponse;
import com.example.timetoeat.domain.notification.entity.NotificationCategory;
import com.example.timetoeat.domain.notification.entity.NotificationType;
import com.example.timetoeat.domain.notification.service.NotificationCommandService;
import com.example.timetoeat.domain.notification.service.NotificationQueryService;
import com.example.timetoeat.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 알림함(SCR-25)·동네 소식(SCR-26). 기획 문서 §14.
 * GET    /api/v1/notifications?category=ALARM|LOCAL_NEWS&newsType=SCHOOL_NEWS&newsType=...&size=50
 * GET    /api/v1/notifications/unread-count
 * PATCH  /api/v1/notifications/{id}/read
 * DELETE /api/v1/notifications/{id}
 */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class NotificationController {

    private final NotificationQueryService queryService;
    private final NotificationCommandService commandService;

    @GetMapping
    public ApiResponse<List<NotificationResponse>> list(
            @AuthenticationPrincipal(expression = "memberId") Long meId,
            @RequestParam(defaultValue = "ALARM") NotificationCategory category,
            @RequestParam(name = "newsType", required = false) List<NotificationType> newsTypes,
            @RequestParam(defaultValue = "50") int size
    ) {
        return ApiResponse.success(queryService.list(meId, category, newsTypes, size));
    }

    @GetMapping("/unread-count")
    public ApiResponse<UnreadCountResponse> unreadCount(@AuthenticationPrincipal(expression = "memberId") Long meId) {
        return ApiResponse.success(new UnreadCountResponse(queryService.unreadAlarmCount(meId)));
    }

    @PatchMapping("/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void read(@AuthenticationPrincipal(expression = "memberId") Long meId, @PathVariable Long id) {
        commandService.markRead(meId, id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal(expression = "memberId") Long meId, @PathVariable Long id) {
        commandService.delete(meId, id);
    }
}
