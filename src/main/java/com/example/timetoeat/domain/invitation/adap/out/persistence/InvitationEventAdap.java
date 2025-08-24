package com.example.timetoeat.domain.invitation.adap.out.persistence;

import com.example.timetoeat.domain.invitation.application.port.out.PublishEventPort;
import com.example.timetoeat.global.util.DomainEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import static com.example.timetoeat.global.config.RabbitMqConfig.DOMAIN_EVENTS_EXCHANGE;

@Component
@RequiredArgsConstructor
public class InvitationEventAdap implements PublishEventPort {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publishEvent(DomainEvent domainEvent) {
        rabbitTemplate.convertAndSend(DOMAIN_EVENTS_EXCHANGE,domainEvent.getRoutingKey(), domainEvent);
    }
}
