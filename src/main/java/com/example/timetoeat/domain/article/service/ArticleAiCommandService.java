package com.example.timetoeat.domain.article.service;

import com.example.timetoeat.domain.article.dto.request.AiInferenceUpsertRequest;
import com.example.timetoeat.domain.article.entity.Article;
import com.example.timetoeat.domain.article.entity.MealLog;
import com.example.timetoeat.domain.article.exception.ArticleErrorCode;
import com.example.timetoeat.domain.article.repository.ArticleRepository;
import com.example.timetoeat.domain.article.repository.ArticleTagRepository;
import com.example.timetoeat.domain.article.repository.MealLogRepository;
import com.example.timetoeat.global.error.exception.CustomException;
import com.example.timetoeat.domain.member.entity.MemberEntity;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class ArticleAiCommandService {

    private final ArticleRepository articleRepository;
    private final MealLogRepository mealLogRepository;
    private final ArticleTagRepository articleTagRepository;
    private final EntityManager em;

    public void upsertMealLog(Long articleId, AiInferenceUpsertRequest req) {

        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new CustomException(ArticleErrorCode.ARTICLE_NOT_FOUND));

        Instant tsUtc = req.getTimestamp().withOffsetSameInstant(ZoneOffset.UTC).toInstant();

        // 글 작성자 + 태그된 모든 사용자
        Set<Long> targetMemberIds = new LinkedHashSet<>();
        targetMemberIds.add(article.getAuthor().getId());
        targetMemberIds.addAll(articleTagRepository.findTaggedMemberIds(articleId));

        for (Long memberId : targetMemberIds) {
            upsertOne(article, memberId, req, tsUtc);
        }
    }

    private void upsertOne(Article article, Long memberId, AiInferenceUpsertRequest req, Instant tsUtc) {
        mealLogRepository.findByArticle_IdAndMember_Id(article.getId(), memberId)
                .ifPresentOrElse(log -> {
                    if (log.getTsUtc().isAfter(tsUtc)) return; // 더 최신이면 무시
                    boolean same = req.getCode().equals(log.getCode())
                            && req.getLabel().equals(log.getLabel())
                            && Double.compare(req.getConfidence(), log.getConfidence()) == 0
                            && tsUtc.equals(log.getTsUtc());
                    if (same) return;
                    log.update(req.getCode(), req.getLabel(), req.getConfidence(), tsUtc);
                }, () -> {
                    var memberRef = em.getReference(MemberEntity.class, memberId);
                    MealLog newLog = MealLog.builder()
                            .article(article)
                            .member(memberRef) // 태그된 사용자 각각으로 저장
                            .code(req.getCode())
                            .label(req.getLabel())
                            .confidence(req.getConfidence())
                            .tsUtc(tsUtc)
                            .build();
                    mealLogRepository.save(newLog);
                });
    }
}
