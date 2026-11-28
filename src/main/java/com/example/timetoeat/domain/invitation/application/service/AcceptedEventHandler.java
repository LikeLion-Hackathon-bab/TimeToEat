package com.example.timetoeat.domain.invitation.application.service;

import com.example.timetoeat.domain.invitation.domain.event.InvitationAcceptedEvent;
import com.example.timetoeat.global.util.DomainEvent;
import com.example.timetoeat.global.common.EventHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


@Slf4j
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
        // 리스너에서 예외를 던지면 기본 설정(default-requeue-rejected=true)으로 즉시 무한 재전달된다.
        // 대기실은 두 사람이 POST /invitations/{id}/enter 할 때 다시 준비되므로 여기서는 기록만 한다.
        try {
            rooms.prepare(event.invitationMatchingResult().invitation());
        } catch (RuntimeException e) {
            log.warn("초대 {} 대기실 사전 준비 실패(입장 시 재시도): {}",
                    event.invitationMatchingResult().invitationId(), e.getMessage());
        }
    }
}
