package com.example.timetoeat.qa;

import com.example.timetoeat.domain.invitation.adap.out.persistence.InvitationEntity;
import com.example.timetoeat.domain.invitation.adap.out.persistence.InvitationRepository;
import com.example.timetoeat.domain.invitation.domain.InvitationStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 재리뷰 L: 초대 행 잠금(findForUpdate)이 보낸·받은 회원 행까지 잠그지 않는다.
 * join fetch + FOR UPDATE 였을 때는 A→B, B→A 초대 결과가 동시에 오면 member 행을 반대 순서로 잡아 데드락이 날 수 있었다.
 */
class InvitationLockScopeIT extends QaIntegrationSupport {
    @Autowired InvitationRepository invitations;
    @Autowired TransactionTemplate tx;

    @Test
    void lockingAnInvitationLeavesMemberRowsFree() throws Exception {
        var a = member("가"); var b = member("나");
        long id = invitations.save(new InvitationEntity(null, a, b, "밥", InvitationStatus.APPROVED, LocalDateTime.now())).getId();
        tx.executeWithoutResult(s -> {
            var inv = invitations.findForUpdate(id).orElseThrow();
            assertEquals(a.getId(), inv.getInviter().getId()); // 지연 프록시라도 id는 읽힌다
            // 다른 연결에서 회원 행을 NOWAIT로 잠가 본다. 잠겨 있으면 즉시 오류(3572).
            var other = CompletableFuture.supplyAsync(() -> tx.execute(t2 ->
                    jdbc.queryForList("select id from member where id in (?, ?) for update nowait", Long.class, a.getId(), b.getId()).size()));
            try {
                assertEquals(2, other.get(10, TimeUnit.SECONDS));
            } catch (Exception e) {
                fail("초대 잠금이 회원 행까지 잠갔다: " + e);
            }
            // 초대 행 자체는 잠겨 있어야 한다
            var same = CompletableFuture.supplyAsync(() -> tx.execute(t3 -> {
                try { return jdbc.queryForList("select id from invitation where id=? for update nowait", Long.class, id).size(); }
                catch (org.springframework.dao.DataAccessException locked) { return -1; }
            }));
            try { assertEquals(-1, same.get(10, TimeUnit.SECONDS)); } catch (Exception e) { fail(e); }
        });
    }
}
