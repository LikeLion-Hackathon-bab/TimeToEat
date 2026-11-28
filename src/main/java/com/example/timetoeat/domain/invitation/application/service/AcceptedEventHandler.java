package com.example.timetoeat.domain.invitation.application.service;

import com.example.timetoeat.domain.invitation.domain.event.InvitationAcceptedEvent;
import com.example.timetoeat.global.util.DomainEvent;
import com.example.timetoeat.global.common.EventHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class AcceptedEventHandler implements EventHandler<InvitationAcceptedEvent> {

    private final InvitationRoomService rooms;

    @Override
    public boolean supports(DomainEvent event) {
        return event instanceof InvitationAcceptedEvent;
    }

    @Override
    public void handle(InvitationAcceptedEvent event) {
        rooms.prepare(event.invitationMatchingResult().invitation());
    }
}
