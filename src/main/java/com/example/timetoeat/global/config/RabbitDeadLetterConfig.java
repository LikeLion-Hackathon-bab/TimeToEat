package com.example.timetoeat.global.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 리스너 재시도(application-prod.yml)를 다 쓴 메시지를 버리지 않고 dead-letter 큐에 보관한다.
 * 예외 메시지·스택은 헤더(x-exception-*)에 붙는다. 원인을 고친 뒤 관리 화면에서 원래 교환기로 다시 보낼 수 있다.
 *
 * 기존 큐에 x-dead-letter 인자를 다는 방식은 이미 만들어진 큐와 인자가 달라 선언이 실패하므로 쓰지 않는다.
 * Spring Boot 는 MessageRecoverer 빈이 있으면 리스너 재시도에 이것을 쓴다.
 */
@Configuration
public class RabbitDeadLetterConfig {
    public static final String DEAD_LETTER_EXCHANGE = "domain-events-dlx";
    public static final String DEAD_LETTER_QUEUE = "domain-events-dead-letter";
    public static final String DEAD_LETTER_KEY = "dead";

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DEAD_LETTER_EXCHANGE);
    }

    @Bean
    public Queue deadLetterQueue() {
        return new Queue(DEAD_LETTER_QUEUE);
    }

    @Bean
    public Binding deadLetterBinding(Queue deadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with(DEAD_LETTER_KEY);
    }

    @Bean
    public MessageRecoverer deadLetterRecoverer(RabbitTemplate rabbitTemplate) {
        return new RepublishMessageRecoverer(rabbitTemplate, DEAD_LETTER_EXCHANGE, DEAD_LETTER_KEY);
    }
}
