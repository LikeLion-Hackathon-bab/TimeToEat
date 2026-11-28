package com.example.timetoeat.global.event;

import com.example.timetoeat.global.util.DomainEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.example.timetoeat.global.config.RabbitMqConfig.DOMAIN_EVENTS_EXCHANGE;

/**
 * 도메인 이벤트를 트랜잭션 커밋 뒤에만 브로커로 보낸다.
 * 커밋 전에 보내면 소비자가 아직 커밋되지 않은 참여 행을 못 본 채 Socket 방을 만들고(첫 요청만 반영),
 * 커밋이 실패해도 이벤트는 이미 나간 상태가 된다.
 * 커밋 뒤 발행이 실패하면 DB 상태는 유지하고 오류만 기록한다. 대기실은 참가자가
 * POST /meetings/rooms/{id}/enter, /invitations/{id}/enter 로 들어올 때 다시 준비된다.
 *
 * 발행은 요청 스레드가 아니라 전용 단일 스레드에서 한다(R18-1: 브로커가 멈추면 마감 참여 응답이 20초 넘게 걸렸다).
 * 스레드가 하나라 이 JVM 안의 발행 순서(커밋 순서)는 그대로 지켜진다. 대기열이 넘치면 버리고 오류만 기록한다.
 */
@Slf4j
@Component
public class AfterCommitPublisher implements DisposableBean {
    static final int QUEUE_CAPACITY = 1_000;

    private final RabbitTemplate rabbitTemplate;
    private final ExecutorService executor;

    public AfterCommitPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
        AtomicInteger seq = new AtomicInteger();
        this.executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(QUEUE_CAPACITY),
                r -> {
                    Thread t = new Thread(r, "domain-event-publisher-" + seq.incrementAndGet());
                    t.setDaemon(true);
                    return t;
                },
                new ThreadPoolExecutor.AbortPolicy());
    }

    public void publish(DomainEvent event) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    dispatch(event);
                }
            });
        } else {
            dispatch(event);
        }
    }

    private void dispatch(DomainEvent event) {
        try {
            executor.execute(() -> send(event));
        } catch (RejectedExecutionException e) {
            log.error("도메인 이벤트 발행 대기열 초과/종료로 버림(DB는 커밋됨, 대기실 입장 시 복구): {}", event.getRoutingKey());
        }
    }

    private void send(DomainEvent event) {
        try {
            rabbitTemplate.convertAndSend(DOMAIN_EVENTS_EXCHANGE, event.getRoutingKey(), event);
        } catch (RuntimeException e) {
            log.error("도메인 이벤트 발행 실패(DB는 커밋됨, 대기실 입장 시 복구): {} {}", event.getRoutingKey(), e.getMessage(), e);
        }
    }

    /** 종료 시 남은 발행을 잠깐 마저 보낸다(브로커가 멈췄으면 연결 제한 시간 안에 실패하고 끝난다). */
    @Override
    public void destroy() throws InterruptedException {
        executor.shutdown();
        if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
            int dropped = executor.shutdownNow().size();
            log.error("종료 중 보내지 못한 도메인 이벤트 {}건(대기실 입장 시 복구)", dropped);
        }
    }
}
