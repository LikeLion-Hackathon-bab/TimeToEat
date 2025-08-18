package com.example.timetoeat.domain.posting.api.web.post;

import com.example.timetoeat.domain.posting.core.gateway.service.postEvent.PostEventDispatcher;
import com.example.timetoeat.domain.posting.domain.model.postEvent.PostEvent;
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
