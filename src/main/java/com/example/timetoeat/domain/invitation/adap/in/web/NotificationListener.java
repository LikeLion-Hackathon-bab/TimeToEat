package com.example.timetoeat.domain.invitation.adap.in.web;

import com.example.timetoeat.domain.invitation.application.service.EventDispatcher;
import com.example.timetoeat.global.util.DomainEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationListener {

    private final EventDispatcher eventDispatcher;

    @RabbitListener(queues = "notification-queue")
    public void handleEvent(DomainEvent domainEvent) {
        eventDispatcher.dispatch(domainEvent);
    }
}
