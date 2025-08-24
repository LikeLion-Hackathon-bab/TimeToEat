package com.example.timetoeat.domain.invitation.domain.event;

import com.example.timetoeat.domain.invitation.domain.InvitationMatchingResult;

public record InvitationAcceptedEvent(InvitationMatchingResult invitationMatchingResult) implements InvitationEvent {

    @Override
    public String getRoutingKey() {
        return "invitation.event.accepted";
    }
}
