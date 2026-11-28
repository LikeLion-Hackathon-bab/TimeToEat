package com.example.timetoeat.qa;

import com.example.timetoeat.domain.invitation.application.port.in.command.SendInvitationCommand;
import com.example.timetoeat.domain.invitation.application.port.in.usecase.InvitationUseCase;
import com.example.timetoeat.domain.invitation.domain.vo.InvitationId;
import com.example.timetoeat.domain.post.domain.vo.member.MemberId;
import com.example.timetoeat.global.error.exception.CustomException;
import com.example.timetoeat.global.error.exception.CustomExceptionHandler;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/** F40: 같은 보낸이→받는이 PENDING 초대는 동시 요청에도 1건만 저장되고 나머지는 409. */
class DuplicateInvitationIT extends QaIntegrationSupport {
    @Autowired InvitationUseCase invitations;

    private static int status(Runnable r) {
        try { r.run(); return 200; }
        catch (CustomException e) { return e.getBaseError().getHttpStatus().value(); }
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

    private int pending(long inviter, long invitee) {
        return jdbc.queryForObject("select count(*) from invitation where inviter_id=? and invitee_id=? and status='PENDING'",
                Integer.class, inviter, invitee);
    }

    private Runnable send(long from, long to) {
        return () -> invitations.sendInvitation(new MemberId(from), new SendInvitationCommand(new MemberId(to), "밥"));
    }

    @RepeatedTest(3)
    void concurrentDoubleSendStoresOnce() throws Exception {
        var a = member("가"); var b = member("나");
        var tasks = new ArrayList<Runnable>();
        for (int i = 0; i < 6; i++) tasks.add(send(a.getId(), b.getId()));
        assertEquals(List.of(200, 409, 409, 409, 409, 409), together(tasks));
        assertEquals(1, pending(a.getId(), b.getId()));
    }

    @RepeatedTest(3)
    void oppositeDirectionsAtOnceBothSucceedWithoutDeadlock() throws Exception {
        var a = member("가"); var b = member("나");
        assertEquals(List.of(200, 200), together(List.of(send(a.getId(), b.getId()), send(b.getId(), a.getId()))));
        assertEquals(1, pending(a.getId(), b.getId()));
        assertEquals(1, pending(b.getId(), a.getId()));
    }

    @Test
    void conflictBodyAndReinviteAfterDecision() {
        var a = member("가"); var b = member("나");
        send(a.getId(), b.getId()).run();
        var e = assertThrows(CustomException.class, send(a.getId(), b.getId())::run);
        var response = new CustomExceptionHandler().handleBusinessException(e);
        assertEquals(409, response.getStatusCode().value());
        assertEquals("이미 보낸 초대장이 있어요.", response.getBody().getMessage());

        long first = jdbc.queryForObject("select id from invitation where inviter_id=? and invitee_id=?", Long.class, a.getId(), b.getId());
        invitations.rejectInvitation(new MemberId(b.getId()), new InvitationId(first));
        send(a.getId(), b.getId()).run(); // 거절 뒤 재초대는 허용
        long second = jdbc.queryForObject("select max(id) from invitation where inviter_id=? and invitee_id=?", Long.class, a.getId(), b.getId());
        invitations.acceptInvitation(new MemberId(b.getId()), new InvitationId(second));
        send(a.getId(), b.getId()).run(); // 수락 뒤 재초대도 허용
        assertEquals(1, pending(a.getId(), b.getId()));
        assertEquals(3, jdbc.queryForObject("select count(*) from invitation where inviter_id=? and invitee_id=?",
                Integer.class, a.getId(), b.getId()));
    }
}
