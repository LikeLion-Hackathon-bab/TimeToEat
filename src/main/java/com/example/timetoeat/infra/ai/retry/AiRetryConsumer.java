package com.example.timetoeat.infra.ai.retry;

import com.example.timetoeat.domain.article.dto.request.AiInferenceUpsertRequest;
import com.example.timetoeat.domain.article.service.ArticleAiCommandService;
import com.example.timetoeat.infra.ai.AiGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.rabbitmq", name = "enabled", havingValue = "true")
public class AiRetryConsumer {

    private final AiGateway aiGateway;
    private final ArticleAiCommandService articleAiCommandService;

    @RabbitListener(queues = AiRetryConfig.QUEUE)
    public void consume(AiInferRequestMessage msg) {
        var result = aiGateway.inferFoodSync(msg.getArticleId(), msg.getAuthorId(), msg.getImagePath(), msg.getMealAtKst());
        var upsert = AiInferenceUpsertRequest.from(result);

        articleAiCommandService.upsertMealLog(msg.getArticleId(), upsert);
    }
}
