package com.example.timetoeat.qa;

import com.example.timetoeat.domain.participation.application.port.in.usecase.ParticipationUseCase;
import com.example.timetoeat.domain.post.domain.vo.member.MemberId;
import com.example.timetoeat.domain.post.domain.vo.post.PostId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

/**
 * 처리할 수 없는 메시지는 몇 번 재시도 후 버린다. 이전에는 CompletedEventHandler 예외마다 즉시 재큐돼
 * 1분에 수백만 줄 로그가 쌓였다(실제 스택에서 재현).
 */
@DirtiesContext
@TestPropertySource(properties = "spring.rabbitmq.listener.simple.auto-startup=true")
class PoisonMessageNoRequeueIT extends QaIntegrationSupport {
    @Autowired AmqpAdmin admin;
    @Autowired org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry listeners;
    @Autowired ParticipationUseCase participation;

    @BeforeEach @AfterEach
    void purge() {
        // 리스너가 기동 때 미리 가져간(prefetch) 이전 메시지는 purge로 지워지지 않는다. 멈추고 비운 뒤 다시 켠다.
        listeners.stop();
        admin.purgeQueue("matching-queue", false);
        admin.purgeQueue("notification-queue", false);
        listeners.start();
    }

    @Test
    void failingCompletedEventIsRetriedBoundedThenDropped() throws Exception {
        var owner = member("작성자");
        var friend = member("참여자");
        var friend2 = member("참여자2");
        var p = openPost(owner, 2);
        AtomicInteger calls = new AtomicInteger();
        doAnswer(inv -> { calls.incrementAndGet(); throw new IllegalStateException("socket down"); })
                .when(socket).sendAnnouncement(any(), any(), any(), any());

        // 모집 인원(2)은 작성자를 뺀 참여자 수. 두 번째 참여가 마지막 자리 → 완료 이벤트
        participation.apply(new MemberId(friend.getId()), new PostId(p.getId()));
        participation.apply(new MemberId(friend2.getId()), new PostId(p.getId()));

        long deadline = System.currentTimeMillis() + 20_000;
        while (calls.get() == 0 && System.currentTimeMillis() < deadline) Thread.sleep(100);
        Thread.sleep(6_000); // 1s + 2s 재시도 간격보다 넉넉히
        int n = calls.get();
        System.out.println("QA completed-event deliveries = " + n);
        assertTrue(n >= 1, "완료 이벤트가 소비되지 않았다");
        assertTrue(n <= 3, "재시도 상한(3) 초과: " + n);
    }
}
