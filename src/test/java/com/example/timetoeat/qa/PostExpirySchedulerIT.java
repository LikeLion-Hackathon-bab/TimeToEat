package com.example.timetoeat.qa;

import com.example.timetoeat.domain.participation.adap.in.dto.ParticipationSocketDto;
import com.example.timetoeat.domain.participation.application.port.in.usecase.ParticipationUseCase;
import com.example.timetoeat.domain.post.adap.in.scheduler.PostExpiryScheduler;
import com.example.timetoeat.domain.post.application.service.ExpiredEventHandler;
import com.example.timetoeat.domain.post.application.service.PostExpirySweeper;
import com.example.timetoeat.domain.post.domain.event.PostExpiredEvent;
import com.example.timetoeat.domain.post.domain.vo.member.MemberId;
import com.example.timetoeat.domain.post.domain.vo.post.PostId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 검증 14 A5: 아무도 참여를 누르지 않아도 만료 공고가 마감되고 대기실이 열려야 한다.
 * 브로커 대신 발행 시점에 ExpiredEventHandler를 바로 실행해, 만료 → Socket 방 생성 요청까지 한 번에 본다.
 * 발행은 커밋 뒤 별도 스레드에서 일어나므로(R18-1) Socket 호출은 timeout 으로 기다리고, '한 번만'은 after 로 본다.
 */
class PostExpirySchedulerIT extends QaIntegrationSupport {
    @Autowired PostExpirySweeper sweeper;
    @Autowired ExpiredEventHandler expiredHandler;
    @Autowired ParticipationUseCase participation;
    @Autowired ApplicationContext context;
    @MockitoSpyBean RabbitTemplate rabbit;

    @BeforeEach
    void runExpiredHandlerInline() {
        doNothing().when(rabbit).convertAndSend(anyString(), anyString(), any(Object.class)); // 실제 브로커로는 보내지 않는다
        doAnswer(inv -> { expiredHandler.handle((PostExpiredEvent) inv.getArgument(2)); return null; })
                .when(rabbit).convertAndSend(anyString(), eq("post.event.expired"), any(Object.class));
    }

    private String status(long postId) {
        return jdbc.queryForObject("select status from post where id=?", String.class, postId);
    }

    /** 참여자 1명이 있는 2인 공고를 만들고 만료 시각을 1분 전으로 옮긴다(시계 이동 대신). */
    private long expiredPostWithOneParticipant(long[] people) {
        var owner = member("모집자");
        var joiner = member("참여자");
        var post = openPost(owner, 3);
        participation.apply(new MemberId(joiner.getId()), new PostId(post.getId()));
        jdbc.update("update post set expired_at = now() - interval 1 minute where id=?", post.getId());
        people[0] = owner.getId();
        people[1] = joiner.getId();
        return post.getId();
    }

    @Test
    void schedulerBeanIsOffInTests() {
        assertTrue(context.getBeansOfType(PostExpiryScheduler.class).isEmpty());
    }

    @Test
    void expiredPostClosesAndOpensRoomWithoutAnyJoinAttempt() {
        long[] people = new long[2];
        long postId = expiredPostWithOneParticipant(people);
        assertEquals("OPEN", status(postId));

        assertTrue(sweeper.sweep() >= 1);

        assertEquals("CLOSED", status(postId));
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ParticipationSocketDto>> who = ArgumentCaptor.forClass(List.class);
        verify(socket, timeout(10_000).times(1)).sendAnnouncement(eq(new PostId(postId)), any(), who.capture(), any());
        var ids = who.getValue().stream().map(ParticipationSocketDto::userId).toList();
        assertEquals(2, ids.size());
        assertTrue(ids.containsAll(List.of(String.valueOf(people[0]), String.valueOf(people[1]))), ids.toString());

        // 두 번째 스윕은 아무것도 하지 않는다
        sweeper.sweep();
        verify(socket, after(1_000).times(1)).sendAnnouncement(eq(new PostId(postId)), any(), any(), any());
        assertEquals("CLOSED", status(postId));
    }

    @Test
    void concurrentSweepsCreateTheRoomOnce() throws Exception {
        long[] people = new long[2];
        long postId = expiredPostWithOneParticipant(people);
        var pool = Executors.newFixedThreadPool(4);
        var gate = new CountDownLatch(1);
        var futures = new ArrayList<Future<Integer>>();
        for (int i = 0; i < 4; i++) futures.add(pool.submit(() -> { gate.await(); return sweeper.sweep(); }));
        gate.countDown();
        for (var f : futures) f.get(30, TimeUnit.SECONDS);
        pool.shutdown();

        assertEquals("CLOSED", status(postId));
        verify(socket, timeout(10_000).times(1)).sendAnnouncement(eq(new PostId(postId)), any(), any(), any());
        verify(socket, after(1_000).times(1)).sendAnnouncement(eq(new PostId(postId)), any(), any(), any());
    }

    @Test
    void lazyExpiryOnJoinAndSweepTogetherCreateTheRoomOnce() {
        long[] people = new long[2];
        long postId = expiredPostWithOneParticipant(people);
        var late = member("늦은참여자");

        // 참여 시 지연 만료(410) 먼저
        var gone = assertThrows(ResponseStatusException.class,
                () -> participation.apply(new MemberId(late.getId()), new PostId(postId)));
        assertEquals(410, gone.getStatusCode().value());
        assertEquals("CLOSED", status(postId));
        // 그 뒤 스윕은 이미 CLOSED라 건너뛴다
        sweeper.sweep();
        verify(socket, timeout(10_000).times(1)).sendAnnouncement(eq(new PostId(postId)), any(), any(), any());
        verify(socket, after(1_000).times(1)).sendAnnouncement(eq(new PostId(postId)), any(), any(), any());

        // 반대 순서: 스윕이 먼저 마감하면 늦은 참여는 409(이미 마감)이고 방을 다시 만들지 않는다
        long[] others = new long[2];
        long second = expiredPostWithOneParticipant(others);
        sweeper.sweep();
        var conflict = assertThrows(ResponseStatusException.class,
                () -> participation.apply(new MemberId(late.getId()), new PostId(second)));
        assertEquals(409, conflict.getStatusCode().value());
        verify(socket, timeout(10_000).times(1)).sendAnnouncement(eq(new PostId(second)), any(), any(), any());
        verify(socket, after(1_000).times(1)).sendAnnouncement(eq(new PostId(second)), any(), any(), any());
    }

    @Test
    void expiredPostWithoutParticipantsClosesWithoutRoom() {
        var owner = member("혼자");
        var post = openPost(owner, 2);
        jdbc.update("update post set expired_at = now() - interval 1 minute where id=?", post.getId());

        sweeper.sweep();

        assertEquals("CLOSED", status(post.getId()));
        verify(socket, never()).sendAnnouncement(eq(new PostId(post.getId())), any(), any(), any());
    }

    @Test
    void openPostBeforeExpiryIsUntouched() {
        var owner = member("아직");
        var post = openPost(owner, 2);
        sweeper.sweep();
        assertEquals("OPEN", status(post.getId()));
    }
}
