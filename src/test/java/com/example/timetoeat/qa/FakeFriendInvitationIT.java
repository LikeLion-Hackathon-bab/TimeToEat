package com.example.timetoeat.qa;

import com.example.timetoeat.domain.friend.service.FriendMealQueryService;
import com.example.timetoeat.domain.invitation.application.port.in.command.SendInvitationCommand;
import com.example.timetoeat.domain.invitation.application.port.in.usecase.InvitationUseCase;
import com.example.timetoeat.domain.post.domain.vo.member.MemberId;
import com.example.timetoeat.global.error.exception.CustomException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * D31: 친구 목록의 시연용 채움 항목(id 1000+)이 실제 회원 id와 겹칠 수 있고, 그 id로 초대를 보낼 수 있는지.
 * 이제 가짜 채움은 DEMO_FRIENDS_ENABLED(기본 false) 뒤로 숨었고, 0 이하 id 초대는 400이다.
 */
class FakeFriendInvitationIT extends QaIntegrationSupport {
    @Autowired FriendMealQueryService friends;
    @Autowired InvitationUseCase invitations;

    @Test
    void demoEntriesNeverPointAtARealMember() {
        var me = member("나");
        var list = friends.getDemoList(me.getId(), "ALL");
        assertTrue(list.stream().noneMatch(i -> i.getMemberId() <= 0), "기본 설정에서는 가짜 시연 친구(음수 id)가 없다");
        for (var item : list) {
            var real = members.findById(item.getMemberId());
            assertTrue(real.isEmpty() || real.get().getUsername().equals(item.getUserName()),
                    "목록 항목 " + item.getMemberId() + "(" + item.getUserName() + ")이 다른 실제 회원 "
                            + real.map(m -> m.getUsername()).orElse("") + "을 가리킨다");
        }
    }

    @Test
    void invitationToAMissingMemberIsRejectedAndNotStored() {
        var me = member("나");
        int before = jdbc.queryForObject("select count(*) from invitation", Integer.class);
        var bad = assertThrows(CustomException.class, () ->
                invitations.sendInvitation(new MemberId(me.getId()), new SendInvitationCommand(new MemberId(-3L), "밥")));
        assertEquals(400, bad.getBaseError().getHttpStatus().value());
        var e = assertThrows(ResponseStatusException.class, () ->
                invitations.sendInvitation(new MemberId(me.getId()), new SendInvitationCommand(new MemberId(Long.MAX_VALUE), "밥")));
        assertEquals(404, e.getStatusCode().value());
        assertEquals(before, jdbc.queryForObject("select count(*) from invitation", Integer.class));
        assertThrows(IllegalArgumentException.class, () -> // CustomExceptionHandler에서 400
                invitations.sendInvitation(new MemberId(me.getId()), new SendInvitationCommand(new MemberId(me.getId()), "밥")));
    }

}
