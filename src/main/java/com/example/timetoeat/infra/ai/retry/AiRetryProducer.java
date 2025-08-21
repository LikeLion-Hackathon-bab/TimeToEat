package com.example.timetoeat.infra.ai.retry;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.rabbitmq", name = "enabled", havingValue = "true")
public class AiRetryProducer {

    private final RabbitTemplate rabbitTemplate;

    public void sendInfer(Long articleId, Long authorId, String imagePath, LocalDateTime mealAtKst) {
        var msg = new AiInferRequestMessage(articleId, authorId, imagePath, mealAtKst);
        rabbitTemplate.convertAndSend(AiRetryConfig.EXCHANGE, AiRetryConfig.RK, msg);
    }
}
