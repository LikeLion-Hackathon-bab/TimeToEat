package com.example.timetoeat.domain.invitation.application.service;

import com.example.timetoeat.global.util.DomainEvent;
import com.example.timetoeat.global.common.EventHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class EventDispatcher {
    private final List<EventHandler> handlers;

    public void dispatch(DomainEvent event) {
        for (EventHandler handler : handlers) {
            if (handler.supports(event)) {
                handler.handle(event);
            }
        }
    }
}
