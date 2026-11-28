package com.example.timetoeat.domain.notification.service;

import com.example.timetoeat.domain.notification.entity.Notification;
import com.example.timetoeat.domain.notification.entity.NotificationType;
import com.example.timetoeat.domain.notification.repository.NotificationRepository;
import com.example.timetoeat.infra.push.PushMessage;
import com.example.timetoeat.infra.push.PushSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationCommandService {

    private final NotificationRepository repository;
    private final PushSender pushSender;
    private final Clock clock;

    /**
     * 일반 알림 1건 생성. 같은 dedupKey 가 이미 있으면 만들지 않는다(RabbitMQ 재전달 대비).
     * 생성 후 웹 푸시 발송을 시도한다(실패해도 알림 저장은 유지).
     * 트랜잭션을 두지 않는다: 중복 키 충돌을 잡아도 바깥 트랜잭션이 rollback-only 가 되지 않게
     * saveAndFlush 가 자기 트랜잭션에서 끝나도록 한다.
     */
    public void notify(Long receiverId, NotificationType type, String title, String body, String url, String dedupKey) {
        if (receiverId == null) return;
        if (dedupKey != null && repository.existsByDedupKey(dedupKey)) {
            log.debug("notification skipped (duplicate) key={}", dedupKey);
            return;
        }
        try {
            repository.saveAndFlush(Notification.builder()
                    .receiverId(receiverId).type(type).title(title).body(body).url(url).dedupKey(dedupKey)
                    .build());
        } catch (DataIntegrityViolationException duplicate) {
            log.debug("notification skipped (unique) key={}", dedupKey);
            return;
        }
        try {
            pushSender.send(receiverId, new PushMessage(title, body, url));
        } catch (RuntimeException e) {
            log.warn("push send failed receiver={} type={} msg={}", receiverId, type, e.getMessage());
        }
    }

    @Transactional
    public void markRead(Long meId, Long notificationId) {
        repository.findByIdAndReceiverId(notificationId, meId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "알림을 찾을 수 없습니다."))
                .markRead(LocalDateTime.now(clock));
    }

    @Transactional
    public int markAllRead(Long meId) {
        return repository.markAllAlarmsRead(meId, LocalDateTime.now(clock));
    }

    @Transactional
    public void delete(Long meId, Long notificationId) {
        var n = repository.findByIdAndReceiverId(notificationId, meId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "알림을 찾을 수 없습니다."));
        repository.delete(n);
    }
}
