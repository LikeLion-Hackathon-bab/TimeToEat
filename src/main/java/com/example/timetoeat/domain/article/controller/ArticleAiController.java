package com.example.timetoeat.domain.article.controller;

import com.example.timetoeat.domain.article.dto.request.AiInferenceUpsertRequest;
import com.example.timetoeat.domain.article.service.ArticleAiService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/v1/articles/{articleId}/ai")
@RequiredArgsConstructor
public class ArticleAiController {

    private final ArticleAiService articleAiService;

    @Value("${app.ai.api-key:}")
    private String aiApiKey;

    @PostMapping("/inference")
    public ResponseEntity<Void> receiveInference(@PathVariable @Positive Long articleId,
                                                  @RequestHeader("X-AI-KEY") String key,
                                                  @Valid @RequestBody AiInferenceUpsertRequest body) {

        if (!StringUtils.hasText(aiApiKey) || !aiApiKey.equals(key)) {
            return ResponseEntity.status(403).build();
        }

        articleAiService.upsertMealLog(articleId, body);
        return ResponseEntity.noContent().build();
    }
}
