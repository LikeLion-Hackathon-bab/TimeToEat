package com.example.timetoeat.domain.invitation.application.port.out;

import com.example.timetoeat.domain.invitation.domain.Invitation;
import com.example.timetoeat.domain.invitation.domain.vo.InvitationId;
import java.util.Optional;

public interface LoadInvitation {
    Optional<Invitation> findById(InvitationId invitationId);
}
