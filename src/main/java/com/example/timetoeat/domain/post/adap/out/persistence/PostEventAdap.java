package com.example.timetoeat.domain.post.adap.out.persistence;

import com.example.timetoeat.domain.post.application.port.out.PublishPostEvent;
import com.example.timetoeat.global.util.DomainEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import static com.example.timetoeat.global.config.RabbitMqConfig.DOMAIN_EVENTS_EXCHANGE;


@Component
@RequiredArgsConstructor
public class PostEventAdap implements PublishPostEvent {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publish(DomainEvent domainEvent) {
        rabbitTemplate.convertAndSend(DOMAIN_EVENTS_EXCHANGE,domainEvent.getRoutingKey(), domainEvent);
    }
}
