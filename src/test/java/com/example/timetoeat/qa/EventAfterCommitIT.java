package com.example.timetoeat.qa;

import com.example.timetoeat.domain.participation.adap.in.dto.ParticipationSocketDto;
import com.example.timetoeat.domain.participation.application.port.in.usecase.ParticipationUseCase;
import com.example.timetoeat.domain.post.application.service.CompletedEventHandler;
import com.example.timetoeat.domain.post.domain.event.PostCompletedEvent;
import com.example.timetoeat.domain.post.domain.vo.member.MemberId;
import com.example.timetoeat.domain.post.domain.vo.post.PostId;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 마지막 자리 신청 시 PostCompletedEvent가 트랜잭션 커밋 전에 RabbitMQ로 나간다.
 * 소비자(CompletedEventHandler)가 커밋보다 먼저 참여 행을 읽으면 마지막 신청자가 빠진 명단으로 Socket 방이 만들어지고,
 * Socket seedFromAnnouncement는 첫 요청만 반영(room.service.ts:340)하므로 이후 입장(RoomEntryService)으로도 복구되지 않는다.
 * 이 테스트는 "소비자가 커밋보다 빠른" 순서를 결정적으로 만든다: 발행 호출 시점에 다른 스레드에서 실제 핸들러를 실행한다.
 */
class EventAfterCommitIT extends QaIntegrationSupport {
    @Autowired ParticipationUseCase participation;
    @Autowired CompletedEventHandler handler;
    @MockitoSpyBean RabbitTemplate rabbit;

    @Test
    void completionConsumerSeesTheLastApplicant() throws Exception {
        var owner = member("모집자");
        var first = member("첫친구");
        var last = member("마지막친구");
        var post = openPost(owner, 2);
        participation.apply(new MemberId(first.getId()), new PostId(post.getId()));

        AtomicReference<Throwable> failure = new AtomicReference<>();
        doAnswer(inv -> {
            Thread consumer = new Thread(() -> {
                try { handler.handle((PostCompletedEvent) inv.getArgument(2)); }
                catch (Throwable t) { failure.set(t); }
            });
            consumer.start();
            consumer.join(10_000);
            return null; // 실제 브로커로는 보내지 않는다
        }).when(rabbit).convertAndSend(anyString(), eq("post.event.completed"), any(Object.class));

        participation.apply(new MemberId(last.getId()), new PostId(post.getId()));
        assertNull(failure.get());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ParticipationSocketDto>> people = ArgumentCaptor.forClass(List.class);
        verify(socket).sendAnnouncement(eq(new PostId(post.getId())), any(), people.capture(), any());
        var ids = people.getValue().stream().map(ParticipationSocketDto::userId).toList();
        assertEquals(2, participationRows(post.getId()));
        assertTrue(ids.contains(String.valueOf(last.getId())),
                "Socket 방 명단 " + ids + " 에 마지막 신청자 " + last.getId() + " 가 없다");
        assertEquals(3, ids.size());
    }

    /** 브로커 장애: 커밋 뒤 발행이 실패해도 신청은 저장되고 호출자는 성공을 받는다(대기실은 입장 시 복구). */
    @Test
    void brokerOutageAfterCommitKeepsTheSeat() {
        var owner = member("모집자");
        var last = member("마지막");
        var post = openPost(owner, 1);
        doThrow(new org.springframework.amqp.AmqpConnectException(new java.net.ConnectException("broker down")))
                .when(rabbit).convertAndSend(anyString(), anyString(), any(Object.class));
        assertDoesNotThrow(() -> participation.apply(new MemberId(last.getId()), new PostId(post.getId())));
        assertEquals(1, participationRows(post.getId()));
        assertEquals("CLOSED", jdbc.queryForObject("select status from post where id=?", String.class, post.getId()));
    }
}
