package com.example.timetoeat.domain.invitation.application.service;

import com.example.timetoeat.domain.invitation.domain.event.InvitationSentEvent;
import com.example.timetoeat.global.util.DomainEvent;
import com.example.timetoeat.global.common.EventHandler;

public class SentEventHandler implements EventHandler<InvitationSentEvent> {
    @Override
    public boolean supports(DomainEvent event) {
        return event instanceof InvitationSentEvent;
    }

    @Override
    public void handle(InvitationSentEvent event) {

    }
}
