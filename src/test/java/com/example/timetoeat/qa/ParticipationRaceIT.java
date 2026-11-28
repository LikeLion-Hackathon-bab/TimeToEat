package com.example.timetoeat.qa;

import com.example.timetoeat.domain.participation.application.port.in.usecase.ParticipationUseCase;
import com.example.timetoeat.domain.post.application.port.in.usecase.PostUseCase;
import com.example.timetoeat.domain.post.domain.Status;
import com.example.timetoeat.domain.post.domain.vo.member.MemberId;
import com.example.timetoeat.domain.post.domain.vo.post.PostId;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

/** 실제 MySQL(비관적 잠금)+Redis(Redisson) 위에서 공고 경합. 결과는 HTTP 상태로 환산한다. */
class ParticipationRaceIT extends QaIntegrationSupport {
    @Autowired ParticipationUseCase participation;
    @Autowired PostUseCase postUseCase;

    private static int status(Runnable r) {
        try { r.run(); return 200; }
        catch (ResponseStatusException e) { return e.getStatusCode().value(); }
        catch (RuntimeException e) { return 500; }
    }

    private List<Integer> together(List<Runnable> tasks) throws Exception {
        var pool = Executors.newFixedThreadPool(tasks.size());
        var gate = new CountDownLatch(1);
        var futures = new ArrayList<Future<Integer>>();
        for (var t : tasks) futures.add(pool.submit(() -> { gate.await(); return status(t); }));
        gate.countDown();
        var out = new ArrayList<Integer>();
        for (var f : futures) out.add(f.get(30, TimeUnit.SECONDS));
        pool.shutdown();
        Collections.sort(out);
        return out;
    }

    private String status(long postId) {
        return jdbc.queryForObject("select status from post where id=?", String.class, postId);
    }

    @RepeatedTest(5)
    void lastSeatGoesToExactlyOnePerson() throws Exception {
        var owner = member("모집자");
        var p = openPost(owner, 1);
        var people = List.of(member("a"), member("b"), member("c"), member("d"));
        var tasks = people.stream().<Runnable>map(m -> () ->
                participation.apply(new MemberId(m.getId()), new PostId(p.getId()))).toList();
        var result = together(tasks);
        assertEquals(List.of(200, 409, 409, 409), result);
        assertEquals(1, participationRows(p.getId()));
        assertEquals("CLOSED", status(p.getId()));
    }

    @RepeatedTest(5)
    void samePersonTwiceIsStoredOnce() throws Exception {
        var owner = member("모집자");
        var p = openPost(owner, 2);
        var me = member("중복");
        Runnable apply = () -> participation.apply(new MemberId(me.getId()), new PostId(p.getId()));
        assertEquals(List.of(200, 409), together(List.of(apply, apply)));
        assertEquals(1, participationRows(p.getId()));
        assertEquals("OPEN", status(p.getId()));
    }

    @RepeatedTest(5)
    void manualCloseAndLastApplicationNeverBothWin() throws Exception {
        var owner = member("모집자");
        var p = openPost(owner, 1);
        var friend = member("친구");
        var result = together(List.of(
                () -> participation.apply(new MemberId(friend.getId()), new PostId(p.getId())),
                () -> postUseCase.close(new MemberId(owner.getId()), new PostId(p.getId()))));
        assertEquals(List.of(200, 409), result);
        assertEquals("CLOSED", status(p.getId()));
        assertTrue(participationRows(p.getId()) <= 1);
    }

    @Test
    void ownershipAndExpiryContract() {
        var owner = member("모집자");
        var other = member("남");
        var p = openPost(owner, 2);
        assertEquals(403, status(() -> postUseCase.close(new MemberId(other.getId()), new PostId(p.getId()))));
        assertEquals(403, status(() -> participation.apply(new MemberId(owner.getId()), new PostId(p.getId()))));
        var expired = savePost(owner, 2, Status.OPEN, LocalDateTime.now().minusSeconds(1), LocalDateTime.now().plusDays(1));
        assertEquals(410, status(() -> participation.apply(new MemberId(other.getId()), new PostId(expired.getId()))));
        assertEquals("CLOSED", status(expired.getId()));
        assertEquals(0, participationRows(expired.getId()));
        assertEquals(409, status(() -> postUseCase.close(new MemberId(owner.getId()), new PostId(expired.getId()))));
    }
}
