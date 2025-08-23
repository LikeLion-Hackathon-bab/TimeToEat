package com.example.timetoeat.domain.post.adap.out.persistence;

import com.example.timetoeat.domain.post.application.port.out.PublishPostEvent;
import com.example.timetoeat.domain.post.domain.PostEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import static com.example.timetoeat.global.config.RabbitMqConfig.EXCHANGE_NAME;

@Component
@RequiredArgsConstructor
public class PostEventPublisher implements PublishPostEvent {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publish(PostEvent postEvent) {
        rabbitTemplate.convertAndSend(EXCHANGE_NAME, postEvent.getRoutingKey(), postEvent);
    }
}
