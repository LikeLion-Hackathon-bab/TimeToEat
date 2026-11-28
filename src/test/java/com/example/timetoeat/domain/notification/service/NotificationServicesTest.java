package com.example.timetoeat.domain.notification.service;

import com.example.timetoeat.domain.notification.entity.Notification;
import com.example.timetoeat.domain.notification.entity.NotificationCategory;
import com.example.timetoeat.domain.notification.entity.NotificationType;
import com.example.timetoeat.domain.notification.repository.NotificationRepository;
import com.example.timetoeat.infra.push.PushMessage;
import com.example.timetoeat.infra.push.PushSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;

import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** F25·F27·F87 알림 생성(중복 방지·푸시 실패 격리)·읽음·삭제·목록(분류·크기 제한). */
class NotificationServicesTest {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 28, 12, 0);

    private NotificationRepository repo;
    private PushSender push;
    private NotificationCommandService command;
    private NotificationQueryService query;

    @BeforeEach
    void setUp() {
        repo = mock(NotificationRepository.class);
        push = mock(PushSender.class);
        command = new NotificationCommandService(repo, push, Clock.fixed(NOW.atZone(KST).toInstant(), KST));
        query = new NotificationQueryService(repo);
    }

    private static Notification alarm(long receiver) {
        return Notification.builder().receiverId(receiver).type(NotificationType.INVITATION_SENT)
                .title("t").body("b").url("/u").build();
    }

    @Test
    void notifySavesThenPushesSameContent() {
        command.notify(5L, NotificationType.INVITATION_ACCEPTED, "제목", "본문", "/invitation/waiting/3", "K1");
        var saved = ArgumentCaptor.forClass(Notification.class);
        verify(repo).saveAndFlush(saved.capture());
        assertEquals(NotificationCategory.ALARM, saved.getValue().getCategory(), "분류는 유형에서 결정");
        assertFalse(saved.getValue().isRead());
        assertEquals("K1", saved.getValue().getDedupKey());
        verify(push).send(5L, new PushMessage("제목", "본문", "/invitation/waiting/3"));
    }

    @Test
    void nullReceiverDoesNothing() {
        command.notify(null, NotificationType.INVITATION_SENT, "t", "b", "/u", "K");
        verifyNoInteractions(repo, push);
    }

    @Test
    void existingDedupKeySkipsSaveAndPush() {
        when(repo.existsByDedupKey("K")).thenReturn(true);
        command.notify(5L, NotificationType.INVITATION_SENT, "t", "b", "/u", "K");
        verify(repo, never()).saveAndFlush(any());
        verifyNoInteractions(push);
    }

    @Test
    void uniqueRaceSkipsPushWithoutThrowing() {
        when(repo.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("uk_notification_dedup_key"));
        assertDoesNotThrow(() -> command.notify(5L, NotificationType.INVITATION_SENT, "t", "b", "/u", "K"));
        verifyNoInteractions(push);
    }

    @Test
    void nullDedupKeyIsNotLookedUp() {
        command.notify(5L, NotificationType.INVITATION_SENT, "t", "b", "/u", null);
        verify(repo, never()).existsByDedupKey(any());
        verify(repo).saveAndFlush(any());
    }

    @Test
    void pushFailureKeepsTheNotification() {
        doThrow(new IllegalStateException("fcm down")).when(push).send(anyLong(), any());
        assertDoesNotThrow(() -> command.notify(5L, NotificationType.INVITATION_SENT, "t", "b", "/u", "K"));
        verify(repo).saveAndFlush(any());
    }

    @Test
    void markReadSetsTimeOnceAndIsIdempotent() {
        var n = alarm(5L);
        when(repo.findByIdAndReceiverId(10L, 5L)).thenReturn(Optional.of(n));
        command.markRead(5L, 10L);
        assertTrue(n.isRead());
        assertEquals(NOW, n.getReadAt());
        n.markRead(NOW.plusHours(1));
        assertEquals(NOW, n.getReadAt(), "두 번째 읽음은 처음 시각 유지");
    }

    @Test
    void othersNotificationIs404ForReadAndDelete() {
        when(repo.findByIdAndReceiverId(10L, 6L)).thenReturn(Optional.empty());
        assertEquals(404, assertThrows(ResponseStatusException.class, () -> command.markRead(6L, 10L)).getStatusCode().value());
        assertEquals(404, assertThrows(ResponseStatusException.class, () -> command.delete(6L, 10L)).getStatusCode().value());
        verify(repo, never()).delete(any());
    }

    @Test
    void deleteRemovesOwnNotification() {
        var n = alarm(5L);
        when(repo.findByIdAndReceiverId(10L, 5L)).thenReturn(Optional.of(n));
        command.delete(5L, 10L);
        verify(repo).delete(n);
    }

    @Test
    void everyTypeHasTheDocumentedCategory() {
        // §14.3 동네 소식 다섯 분류만 LOCAL_NEWS, 나머지(§14.2 일반 알림 15종)는 ALARM
        var news = EnumSet.of(NotificationType.SCHOOL_NEWS, NotificationType.AREA_NEWS, NotificationType.COMMERCIAL_NEWS,
                NotificationType.DISCOUNT_NEWS, NotificationType.MEAL_NEWS);
        for (var t : NotificationType.values())
            assertEquals(news.contains(t) ? NotificationCategory.LOCAL_NEWS : NotificationCategory.ALARM, t.category(), t.name());
        assertEquals(15, NotificationType.values().length - news.size(), "§14.2 일반 알림 15종");
    }

    @Test
    void alarmListClampsPageSize() {
        when(repo.findByReceiverIdAndCategoryOrderByIdDesc(anyLong(), any(), any())).thenReturn(List.of());
        query.list(5L, NotificationCategory.ALARM, null, 0);
        query.list(5L, null, null, 1000);
        query.list(5L, NotificationCategory.ALARM, null, -3);
        var pages = ArgumentCaptor.forClass(Pageable.class);
        verify(repo, times(3)).findByReceiverIdAndCategoryOrderByIdDesc(eq(5L), eq(NotificationCategory.ALARM), pages.capture());
        assertEquals(List.of(1, 100, 1), pages.getAllValues().stream().map(Pageable::getPageSize).toList());
        assertTrue(pages.getAllValues().stream().allMatch(p -> p.getPageNumber() == 0));
    }

    @Test
    void localNewsWithoutFilterAsksForAllFiveNewsTypes() {
        when(repo.findLocalNews(anyLong(), any(), any())).thenReturn(List.of());
        query.list(5L, NotificationCategory.LOCAL_NEWS, null, 20);
        @SuppressWarnings("unchecked") ArgumentCaptor<Collection<NotificationType>> types = ArgumentCaptor.forClass(Collection.class);
        verify(repo).findLocalNews(eq(5L), types.capture(), any());
        assertEquals(EnumSet.of(NotificationType.SCHOOL_NEWS, NotificationType.AREA_NEWS, NotificationType.COMMERCIAL_NEWS,
                NotificationType.DISCOUNT_NEWS, NotificationType.MEAL_NEWS), EnumSet.copyOf(types.getValue()));
    }

    @Test
    void localNewsFilterDropsAlarmTypesAndOnlyAlarmTypesReturnsEmptyWithoutQuery() {
        when(repo.findLocalNews(anyLong(), any(), any())).thenReturn(List.of());
        query.list(5L, NotificationCategory.LOCAL_NEWS, List.of(NotificationType.SCHOOL_NEWS, NotificationType.INVITATION_SENT), 20);
        verify(repo).findLocalNews(eq(5L), eq(List.of(NotificationType.SCHOOL_NEWS)), any());

        assertEquals(List.of(), query.list(5L, NotificationCategory.LOCAL_NEWS, List.of(NotificationType.MATCHING_COMPLETED), 20));
        verify(repo, times(1)).findLocalNews(anyLong(), any(), any());
    }

    @Test
    void unreadCountIsAlarmOnly() {
        when(repo.countByReceiverIdAndCategoryAndReadFalse(5L, NotificationCategory.ALARM)).thenReturn(3L);
        assertEquals(3L, query.unreadAlarmCount(5L));
        verify(repo, never()).countByReceiverIdAndCategoryAndReadFalse(5L, NotificationCategory.LOCAL_NEWS);
    }
}
