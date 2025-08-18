package com.example.timetoeat.domain.posting.infrastructure.web.post;

import com.example.timetoeat.domain.posting.core.application.service.post.MatchingEventHandler;
import com.example.timetoeat.domain.posting.core.domain.model.postEvent.PostEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MatchingListener {
    private final MatchingEventHandler matchingEventHandler;

    @RabbitListener(queues = "matching-queue")
    public void handleMatchingEvent(PostEvent event) {
        if (matchingEventHandler.supports(event.eventType())) {
            matchingEventHandler.handle(event);
        }
    }
}
