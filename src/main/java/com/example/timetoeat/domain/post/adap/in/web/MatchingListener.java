package com.example.timetoeat.domain.post.adap.in.web;

import com.example.timetoeat.domain.post.application.service.MatchingService;
import com.example.timetoeat.domain.post.domain.PostEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MatchingListener {
    private final MatchingService matchingService;

    @RabbitListener(queues = "matching-queue")
    public void handleMatchingEvent(PostEvent event) {
        if (matchingService.supports(event.eventType())) {
            matchingService.handle(event);
        }
    }
}
