package com.example.timetoeat.domain.invitation.application.port.out;

import com.example.timetoeat.domain.invitation.domain.Invitation;
import com.example.timetoeat.domain.invitation.domain.vo.InvitationId;
import com.example.timetoeat.domain.post.domain.vo.member.MemberId;

import java.util.Optional;

public interface LoadInvitation {
    Optional<Invitation> findById(InvitationId invitationId);

    /**
     * 두 회원 행을 id 오름차순으로 FOR UPDATE 잠근다(트랜잭션 끝까지).
     * 같은 쌍의 초대 전송을 직렬화하고, 순서가 고정이라 A→B·B→A 동시 전송도 교착되지 않는다.
     */
    void lockPair(MemberId first, MemberId second);

    boolean existsPending(MemberId inviterId, MemberId inviteeId);
}
