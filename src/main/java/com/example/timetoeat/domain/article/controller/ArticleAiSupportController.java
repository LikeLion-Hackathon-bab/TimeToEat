package com.example.timetoeat.domain.article.controller;

import com.example.timetoeat.domain.article.repository.MealLogRepository;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Clock;
import java.time.Instant;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/ai/support")
@RequiredArgsConstructor
public class ArticleAiSupportController {

    private final MealLogRepository mealLogRepository;
    private final Clock clock;

    @Value("${app.ai.api-key:}")
    private String aiApiKey;

    private void verifyAiKey(String key, Long memberId) {
        if (!StringUtils.hasText(aiApiKey) || !aiApiKey.equals(key)) {
            log.warn("AI support rejected: invalid X-AI-KEY (memberId={})", memberId);
            throw new AccessDeniedException("INVALID_AI_KEY");
        }
    }

    @GetMapping("/users/{memberId}/excluded-codes")
    public java.util.List<String> getExcludedCodes(@RequestHeader("X-AI-KEY") String key,
                                                   @PathVariable @Positive Long memberId,
                                                   @RequestParam(defaultValue = "3") @Positive int days) {
        verifyAiKey(key, memberId);
        Instant since = java.time.Instant.now(clock).minus(java.time.Duration.ofDays(days));

        return mealLogRepository.findCodesSince(memberId, since);
    }
}
