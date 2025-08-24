package com.example.timetoeat.domain.invitation.domain.event;

import com.example.timetoeat.domain.invitation.domain.Invitation;

public record InvitationSentEvent(Invitation invitation) implements InvitationEvent {
    @Override
    public String getRoutingKey() {
        return "invitation.event.sent";
    }
}
