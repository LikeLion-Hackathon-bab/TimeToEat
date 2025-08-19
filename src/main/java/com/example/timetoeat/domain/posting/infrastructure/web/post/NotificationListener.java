package com.example.timetoeat.domain.posting.infrastructure.web.post;

import com.example.timetoeat.domain.posting.core.application.postEvent.PostEventDispatcher;
import com.example.timetoeat.domain.posting.core.domain.model.postEvent.PostEvent;
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
