package com.example.timetoeat.domain.invitation.application.service;

import com.example.timetoeat.domain.invitation.domain.event.InvitationRejectedEvent;
import com.example.timetoeat.global.util.DomainEvent;
import com.example.timetoeat.global.common.EventHandler;

public class RejectedEventHandler implements EventHandler<InvitationRejectedEvent> {
    @Override
    public boolean supports(DomainEvent event) {
        return event instanceof InvitationRejectedEvent;
    }

    @Override
    public void handle(InvitationRejectedEvent event) {

    }
}
