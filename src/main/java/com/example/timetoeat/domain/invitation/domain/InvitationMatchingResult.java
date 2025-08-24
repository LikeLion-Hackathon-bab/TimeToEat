package com.example.timetoeat.domain.invitation.domain;

import com.example.timetoeat.domain.invitation.domain.vo.InvitationId;
import com.example.timetoeat.domain.post.domain.vo.member.MemberId;

import java.util.List;

public record InvitationMatchingResult(
    Invitation invitation,
    InvitationId invitationId,
    List<MemberId> matchedMemberIds
) {
    public static InvitationMatchingResult create(Invitation invitation) {
        if (invitation.status() != InvitationStatus.APPROVED) {
            throw new IllegalArgumentException("Only approved Invitation can be created");
        }
        List<MemberId> memberIds = List.of(invitation.inviteeId(), invitation.inviterId());
        return new InvitationMatchingResult(invitation,invitation.invitationId(), memberIds);
    }
}
