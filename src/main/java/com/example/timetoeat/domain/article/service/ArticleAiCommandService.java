package com.example.timetoeat.domain.article.service;

import com.example.timetoeat.domain.article.dto.request.AiInferenceUpsertRequest;
import com.example.timetoeat.domain.article.entity.Article;
import com.example.timetoeat.domain.article.entity.MealLog;
import com.example.timetoeat.domain.article.exception.ArticleErrorCode;
import com.example.timetoeat.domain.article.repository.ArticleRepository;
import com.example.timetoeat.domain.article.repository.MealLogRepository;
import com.example.timetoeat.global.error.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
@Transactional
public class ArticleAiCommandService {

    private final ArticleRepository articleRepository;
    private final MealLogRepository mealLogRepository;

    public void upsertMealLog(Long articleId, AiInferenceUpsertRequest req) {

        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new CustomException(ArticleErrorCode.ARTICLE_NOT_FOUND));

        Instant tsUtc = req.getTimestamp().withOffsetSameInstant(ZoneOffset.UTC).toInstant();

        mealLogRepository.findByArticle_Id(articleId)
                .ifPresentOrElse(log -> {
                    // 기존 로그가 더 최신이면 무시
                    if (log.getTsUtc().isAfter(tsUtc)) {
                        return;
                    }
                    // 동일 값이면 스킵하여 불필요한 쓰기 방지
                    boolean same =
                            req.getCode().equals(log.getCode()) &&
                            req.getLabel().equals(log.getLabel()) &&
                            Double.compare(req.getConfidence(), log.getConfidence()) == 0 &&
                            tsUtc.equals(log.getTsUtc());
                    if (same) return;

                    log.update(req.getCode(), req.getLabel(), req.getConfidence(), tsUtc);
                }, () -> {
                    // 신규 생성
                    MealLog newLog = MealLog.builder()
                            .article(article)
                            .member(article.getAuthor())
                            .code(req.getCode())
                            .label(req.getLabel())
                            .confidence(req.getConfidence())
                            .tsUtc(tsUtc)
                            .build();
                    mealLogRepository.save(newLog);
                });
    }
}
