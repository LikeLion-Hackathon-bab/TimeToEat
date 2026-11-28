package com.example.timetoeat.qa;

import com.example.timetoeat.domain.invitation.application.port.out.PublishEventPort;
import com.example.timetoeat.domain.invitation.domain.Invitation;
import com.example.timetoeat.domain.invitation.domain.InvitationMatchingResult;
import com.example.timetoeat.domain.invitation.domain.InvitationStatus;
import com.example.timetoeat.domain.invitation.domain.event.InvitationAcceptedEvent;
import com.example.timetoeat.domain.invitation.domain.vo.InvitationId;
import com.example.timetoeat.domain.post.domain.vo.member.MemberId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Socket이 내려가 있을 때 초대 수락 이벤트 소비가 실패하면 AcceptedEventHandler가 ResponseStatusException(503)을 던지고,
 * Spring AMQP 기본값(default-requeue-rejected=true)으로 메시지가 즉시 재전달되어 무한 반복된다.
 */
@DirtiesContext
@org.springframework.test.context.TestPropertySource(properties = "spring.rabbitmq.listener.simple.auto-startup=true")
class AcceptedRequeueIT extends QaIntegrationSupport {
    @Autowired PublishEventPort publisher;
    @Autowired AmqpAdmin admin;

    @BeforeEach @AfterEach
    void purge() {
        admin.purgeQueue("matching-queue", false);
        admin.purgeQueue("notification-queue", false);
    }

    @Test
    void socketOutageDoesNotRedeliverForever() throws Exception {
        var inviter = member("보낸사람");
        var invitee = member("받는사람");
        var counter = new java.util.concurrent.atomic.AtomicInteger();
        doAnswer(inv -> { counter.incrementAndGet(); throw new IllegalStateException("socket down"); })
                .when(socket).ensureInvitation(any(), any(), any());
        var invitation = new Invitation(new InvitationId(9_000L + inviter.getId()), new MemberId(inviter.getId()),
                new MemberId(invitee.getId()), "점심", InvitationStatus.APPROVED, LocalDateTime.now());
        publisher.publishEvent(new InvitationAcceptedEvent(InvitationMatchingResult.create(invitation)));

        long deadline = System.currentTimeMillis() + 20_000; // 리스너 컨테이너 기동 대기
        while (counter.get() == 0 && System.currentTimeMillis() < deadline) Thread.sleep(100);
        Thread.sleep(3_000);
        int calls = counter.get();
        System.out.println("QA socket calls in 3s = " + calls);
        // 바인딩된 큐 2개(matching, notification)에서 한 번씩: 최대 2회가 정상
        assertTrue(calls >= 1, "이벤트가 소비되지 않았다");
        assertTrue(calls <= 2, "3초 동안 Socket 호출 " + calls + "회 (재전달 반복)");
    }
}
