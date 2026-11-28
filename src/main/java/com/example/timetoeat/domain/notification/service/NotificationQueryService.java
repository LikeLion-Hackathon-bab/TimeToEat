package com.example.timetoeat.domain.notification.service;

import com.example.timetoeat.domain.notification.dto.NotificationResponse;
import com.example.timetoeat.domain.notification.entity.NotificationCategory;
import com.example.timetoeat.domain.notification.entity.NotificationType;
import com.example.timetoeat.domain.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationQueryService {

    private static final int MAX_SIZE = 100;

    private final NotificationRepository repository;

    public List<NotificationResponse> list(Long meId, NotificationCategory category,
                                           Collection<NotificationType> newsTypes, int size) {
        var page = PageRequest.of(0, Math.max(1, Math.min(size, MAX_SIZE)));
        if (category == NotificationCategory.LOCAL_NEWS) {
            Collection<NotificationType> types = (newsTypes == null || newsTypes.isEmpty())
                    ? Arrays.stream(NotificationType.values())
                        .filter(t -> t.category() == NotificationCategory.LOCAL_NEWS).toList()
                    : newsTypes.stream().filter(t -> t.category() == NotificationCategory.LOCAL_NEWS).toList();
            if (types.isEmpty()) return List.of();
            return repository.findLocalNews(meId, types, page).stream().map(NotificationResponse::from).toList();
        }
        return repository.findByReceiverIdAndCategoryOrderByIdDesc(meId, NotificationCategory.ALARM, page)
                .stream().map(NotificationResponse::from).toList();
    }

    public long unreadAlarmCount(Long meId) {
        return repository.countByReceiverIdAndCategoryAndReadFalse(meId, NotificationCategory.ALARM);
    }
}
