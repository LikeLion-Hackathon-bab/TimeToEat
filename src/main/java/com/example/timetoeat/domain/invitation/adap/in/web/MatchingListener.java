package com.example.timetoeat.domain.invitation.adap.in.web;

import com.example.timetoeat.domain.invitation.application.service.EventDispatcher;
import com.example.timetoeat.global.util.DomainEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MatchingListener {

    private final EventDispatcher eventDispatcher;

    @RabbitListener(queues = "matching-queue")
    public void handleEvent(DomainEvent event) {
        eventDispatcher.dispatch(event);
    }
}
