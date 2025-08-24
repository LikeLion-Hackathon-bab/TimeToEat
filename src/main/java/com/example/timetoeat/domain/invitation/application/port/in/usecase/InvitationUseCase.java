package com.example.timetoeat.domain.invitation.application.port.in.usecase;

import com.example.timetoeat.domain.invitation.application.port.in.command.SendInvitationCommand;
import com.example.timetoeat.domain.invitation.domain.vo.InvitationId;
import com.example.timetoeat.domain.post.domain.vo.member.MemberId;

public interface InvitationUseCase {
    void sendInvitation(MemberId inviterId, SendInvitationCommand command);
    String acceptInvitation(MemberId inviterId, InvitationId invitedId);
    void rejectInvitation(MemberId inviterId, InvitationId invitedId);
}
