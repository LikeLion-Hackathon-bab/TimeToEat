package com.example.timetoeat.domain.posting.infrastructure.persistence.postEvent;

import com.example.timetoeat.domain.posting.core.application.gateway.postEvent.PostEventPort;
import com.example.timetoeat.domain.posting.core.domain.model.postEvent.PostEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import static com.example.timetoeat.global.config.RabbitMqConfig.EXCHANGE_NAME;

@Component
@RequiredArgsConstructor
public class PostEventAdapter implements PostEventPort {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publish(PostEvent postEvent) {
        rabbitTemplate.convertAndSend(EXCHANGE_NAME, postEvent.getRoutingKey(), postEvent);
    }
}
