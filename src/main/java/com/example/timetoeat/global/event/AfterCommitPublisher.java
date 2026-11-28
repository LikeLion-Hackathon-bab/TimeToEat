package com.example.timetoeat.global.event;

import com.example.timetoeat.global.util.DomainEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static com.example.timetoeat.global.config.RabbitMqConfig.DOMAIN_EVENTS_EXCHANGE;

/**
 * 도메인 이벤트를 트랜잭션 커밋 뒤에만 브로커로 보낸다.
 * 커밋 전에 보내면 소비자가 아직 커밋되지 않은 참여 행을 못 본 채 Socket 방을 만들고(첫 요청만 반영),
 * 커밋이 실패해도 이벤트는 이미 나간 상태가 된다.
 * 커밋 뒤 발행이 실패하면 DB 상태는 유지하고 오류만 기록한다. 대기실은 참가자가
 * POST /meetings/rooms/{id}/enter, /invitations/{id}/enter 로 들어올 때 다시 준비된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AfterCommitPublisher {
    private final RabbitTemplate rabbitTemplate;

    public void publish(DomainEvent event) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send(event);
                }
            });
        } else {
            send(event);
        }
    }

    private void send(DomainEvent event) {
        try {
            rabbitTemplate.convertAndSend(DOMAIN_EVENTS_EXCHANGE, event.getRoutingKey(), event);
        } catch (RuntimeException e) {
            log.error("도메인 이벤트 발행 실패(DB는 커밋됨, 대기실 입장 시 복구): {} {}", event.getRoutingKey(), e.getMessage(), e);
        }
    }
}
