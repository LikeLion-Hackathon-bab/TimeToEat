package com.example.timetoeat.infra.ai.retry;

import lombok.Getter;
import org.springframework.amqp.core.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "app.rabbitmq", name = "enabled", havingValue = "true")
@Getter
public class AiRetryConfig {

    public static final String EXCHANGE = "ai.infer.exchange";
    public static final String QUEUE = "ai.infer.queue";
    public static final String RK = "ai.infer.key";

    @Bean
    public DirectExchange aiInferExchange() {

        return new DirectExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue aiInferQueue() {

        return QueueBuilder.durable(QUEUE).build();
    }

    @Bean
    public Binding aiInferBinding() {
        return BindingBuilder.bind(aiInferQueue()).to(aiInferExchange()).with(RK);
    }

}
