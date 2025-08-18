package com.example.timetoeat.domain.posting.core.gateway.service.postEvent;

import com.example.timetoeat.domain.posting.domain.model.postEvent.PostEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PostEventDispatcher {
    private final List<PostEventHandler> handlers;

    public void dispatch(PostEvent event) {
        handlers.stream()
                .filter(handler -> handler.supports(event.eventType()))
                .findFirst()
                .ifPresent(handler -> handler.handle(event));
    }
}
