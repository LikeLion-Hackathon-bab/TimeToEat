package com.example.timetoeat.domain.invitation.adap.in.web;

import com.example.timetoeat.domain.notification.service.NotificationEventRecorder;
import com.example.timetoeat.global.util.DomainEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * notification-queue 소비자. 알림 저장만 한다.
 * (이전에는 EventDispatcher 로 모든 EventHandler 를 불러 matching-queue 와 같은 처리 —
 *  공고 마감 알림 송신·초대방 준비 — 가 이벤트마다 두 번 실행됐다.)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationListener {

    private final NotificationEventRecorder recorder;

    @RabbitListener(queues = "notification-queue")
    public void handleEvent(DomainEvent domainEvent) {
        recorder.record(domainEvent);
    }
}
