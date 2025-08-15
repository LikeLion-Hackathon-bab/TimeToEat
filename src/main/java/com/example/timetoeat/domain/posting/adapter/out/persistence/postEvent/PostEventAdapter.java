package com.example.timetoeat.domain.posting.adapter.out.persistence.postEvent;

import com.example.timetoeat.domain.posting.application.port.out.postEvent.PostEventPort;
import com.example.timetoeat.domain.posting.domain.model.postEvent.PostEvent;
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
