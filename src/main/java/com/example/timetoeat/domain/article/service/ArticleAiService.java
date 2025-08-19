package com.example.timetoeat.domain.article.service;

import com.example.timetoeat.domain.article.dto.request.AiInferenceUpsertRequest;
import com.example.timetoeat.domain.article.entity.Article;
import com.example.timetoeat.domain.article.entity.MealLog;
import com.example.timetoeat.domain.article.repository.ArticleRepository;
import com.example.timetoeat.domain.article.repository.MealLogRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
@Transactional
public class ArticleAiService {

    private final ArticleRepository articleRepository;
    private final MealLogRepository mealLogRepository;

    public void upsertMealLog(Long articleId, AiInferenceUpsertRequest req) {

        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new EntityNotFoundException("ARTICLE_NOT_FOUND"));

        Instant tsUtc = req.getTimestamp().withOffsetSameInstant(ZoneOffset.UTC).toInstant();

        mealLogRepository.findByArticle_Id(articleId)
                .ifPresentOrElse(
                        log -> log.update(req.getCode(), req.getLabel(), req.getConfidence(), tsUtc),
                        () -> mealLogRepository.save(
                                MealLog.builder()
                                        .article(article)
                                        .member(article.getAuthor())
                                        .code(req.getCode())
                                        .label(req.getLabel())
                                        .confidence(req.getConfidence())
                                        .tsUtc(tsUtc)
                                        .build()
                        )
                );
    }
}
