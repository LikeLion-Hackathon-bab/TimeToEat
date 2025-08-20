/*
package com.example.timetoeat.infra.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.OffsetDateTime;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class HttpAiGateway implements AiGateway {

    private final WebClient aiWebClient;

    @Value("${app.ai.infer-url}")   private String inferUrl;
    @Value("${app.ai.api-key}")     private String apiKey;

    @Override
    public void requestInference(Long articleId, Long userId, String imageUrl,
                                 OffsetDateTime mealAtKst, String callbackUrl) {

        Map<String, Object> payload = Map.of(
                "article_id", articleId,
                "user_id", String.valueOf(userId),
                "image_url", imageUrl,
                "meal_at", mealAtKst.toString(),
                "callback_url", callbackUrl
        );

        aiWebClient.post()
                .uri(inferUrl)
                .header("X-AI-KEY", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .toBodilessEntity()
                .subscribe(
                        ok  -> {},                 // 성공시 무시
                        err -> log.warn("AI inference request failed. articleId={}", articleId, err)
                );
    }
}

*/
