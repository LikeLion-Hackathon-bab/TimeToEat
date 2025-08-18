package com.example.timetoeat.domain.posting.core.application.service.postEvent;

import com.example.timetoeat.domain.posting.core.domain.model.postEvent.PostEvent;
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
