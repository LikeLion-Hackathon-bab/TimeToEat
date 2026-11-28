package com.example.timetoeat.domain.notification.listener;

import com.example.timetoeat.domain.article.event.ArticleCreatedEvent;
import com.example.timetoeat.domain.notification.event.NotificationEvents.*;
import com.example.timetoeat.domain.notification.service.NotificationTriggerRecorder;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 발생 트랜잭션이 커밋된 뒤에만 알림을 만든다(롤백되면 알림 없음).
 * 저장·웹 푸시는 알림 전용 스레드에서 한다: 요청 스레드가 FCM 응답을 기다리지 않고,
 * AFTER_COMMIT 시점에 아직 묶여 있는 원래 트랜잭션 자원에 저장이 '참여'만 하고 커밋되지 않는 문제(F81)도 피한다.
 * fallbackExecution: 대기실 입장처럼 트랜잭션 없이 끝나는 곳에서도 바로 실행.
 *
 * 실행기는 빈으로 등록하지 않는다(Executor 빈이 생기면 Spring Boot 기본 applicationTaskExecutor 가 빠진다).
 * 큐가 차면 알림을 버리고 경고만 남긴다(알림은 부가 기능이라 본 요청을 막지 않는다).
 */
@Slf4j
@Component
public class NotificationTriggerListener {

    private final NotificationTriggerRecorder recorder;
    private final ThreadPoolTaskExecutor executor;

    public NotificationTriggerListener(NotificationTriggerRecorder recorder) {
        this.recorder = recorder;
        var ex = new ThreadPoolTaskExecutor();
        ex.setThreadNamePrefix("noti-");
        ex.setCorePoolSize(2);
        ex.setMaxPoolSize(4);
        ex.setQueueCapacity(1000);
        ex.setWaitForTasksToCompleteOnShutdown(true);
        ex.setAwaitTerminationSeconds(10);
        ex.setRejectedExecutionHandler((r, pool) -> log.warn("notification dropped: queue full"));
        ex.initialize();
        this.executor = ex;
    }

    @PreDestroy
    void shutdown() {
        executor.shutdown();
    }

    private void later(String what, Runnable task) {
        executor.execute(() -> {
            try {
                task.run();
            } catch (RuntimeException ex) {
                log.warn("notification trigger failed what={} msg={}", what, ex.getMessage());
            }
        });
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(ArticleCreatedEvent e) {
        later("article-created", () -> {
            recorder.onArticleCreated(e.getArticleId(), e.getAuthorId());
            recorder.onChallengeProgress(e.getAuthorId());
        });
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(PostLiked e) { later("post-liked", () -> recorder.onLiked(e)); }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(CommentCreated e) { later("comment-created", () -> recorder.onComment(e)); }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(AnnouncementJoined e) { later("announcement-joined", () -> recorder.onJoined(e)); }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(RoomEntered e) { later("room-entered", () -> recorder.onRoomEntered(e)); }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(MeetingConfirmed e) { later("meeting-confirmed", () -> recorder.onConfirmed(e)); }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(ReferralRedeemed e) { later("referral", () -> recorder.onReferral(e)); }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(CouponIssued e) { later("coupon", () -> recorder.onCoupon(e)); }
}
