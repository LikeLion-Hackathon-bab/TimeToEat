package com.example.timetoeat.domain.post.adap.in.web;

import com.example.timetoeat.domain.post.application.service.PostEventDispatcher;
import com.example.timetoeat.domain.post.domain.PostEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationListener {
    private final PostEventDispatcher postEventDispatcher;

    @RabbitListener(queues = "notification-queue")
    public void handlePostEvent(PostEvent event) {
        postEventDispatcher.dispatch(event);
    }
}
