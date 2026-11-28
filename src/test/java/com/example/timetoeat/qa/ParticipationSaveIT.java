package com.example.timetoeat.qa;

import com.example.timetoeat.domain.participation.application.port.in.usecase.ParticipationUseCase;
import com.example.timetoeat.domain.participation.application.port.out.LoadParticipation;
import com.example.timetoeat.domain.participation.application.port.out.SaveParticipation;
import com.example.timetoeat.domain.post.domain.vo.member.MemberId;
import com.example.timetoeat.domain.post.domain.vo.post.PostId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** ParticipationAdap.save가 매번 공고의 참여 행을 모두 지우고 다시 넣던 동작(env 작업자 관찰). */
class ParticipationSaveIT extends QaIntegrationSupport {
    @Autowired ParticipationUseCase participation;
    @Autowired LoadParticipation load;
    @Autowired SaveParticipation save;
    @Autowired TransactionTemplate tx;

    private List<Long> rowIds(long postId) {
        return jdbc.queryForList("select id from participation where post_id=? order by id", Long.class, postId);
    }

    @Test
    void laterApplicationKeepsEarlierRowsAndOtherPostsUntouched() {
        var owner = member("모집자");
        var a = member("a");
        var b = member("b");
        var p = openPost(owner, 3);
        var other = openPost(owner, 3);
        participation.apply(new MemberId(a.getId()), new PostId(p.getId()));
        participation.apply(new MemberId(a.getId()), new PostId(other.getId()));
        var firstRow = rowIds(p.getId()).get(0);
        var otherRows = rowIds(other.getId());

        participation.apply(new MemberId(b.getId()), new PostId(p.getId()));

        assertEquals(firstRow, rowIds(p.getId()).get(0), "첫 신청자 행 id가 바뀌었다(삭제 후 재삽입)");
        assertEquals(2, rowIds(p.getId()).size());
        assertEquals(otherRows, rowIds(other.getId()));
    }

    /** 잠금 밖에서 오래된 상태로 저장하면 먼저 커밋된 다른 신청자 행을 지운다(잠재 결함; 현재 호출부는 잠금 안에서만 호출). */
    @Test
    void staleSnapshotSaveDoesNotDeleteAnotherApplicant() {
        var owner = member("모집자");
        var a = member("a");
        var b = member("b");
        var p = openPost(owner, 3);
        var id = new PostId(p.getId());
        var staleForA = load.getParticipationByPostId(id);
        var staleForB = load.getParticipationByPostId(id);
        tx.executeWithoutResult(s -> save.save(staleForA.add(new MemberId(a.getId()))));
        tx.executeWithoutResult(s -> save.save(staleForB.add(new MemberId(b.getId()))));
        assertEquals(2, participationRows(p.getId()), "먼저 저장된 신청자 행이 사라졌다");
    }
}
