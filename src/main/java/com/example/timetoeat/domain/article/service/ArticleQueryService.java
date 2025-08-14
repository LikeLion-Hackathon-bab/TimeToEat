package com.example.timetoeat.domain.article.service;

import com.example.timetoeat.domain.article.dto.response.ArticleDetailResponse;
import com.example.timetoeat.domain.article.dto.response.ArticleSummaryResponse;
import com.example.timetoeat.domain.article.entity.Article;
import com.example.timetoeat.domain.article.repository.ArticleLikeRepository;
import com.example.timetoeat.domain.article.repository.ArticleRepository;
import com.example.timetoeat.domain.article.repository.ArticleTagRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArticleQueryService {

    private final ArticleRepository articleRepository;
    private final ArticleLikeRepository likeRepository;
    private final ArticleTagRepository tagRepository;

    public Page<ArticleSummaryResponse> getHomeFeed(Long meId, Pageable pageable, LocalDateTime now) {
        var threshold = now.minusHours(24);
        var sorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));

        Page<Article> page = articleRepository.findByCreatedAtGreaterThanEqual(threshold, sorted);

        return mapToSummaryPage(meId, page);
    }

    public Page<ArticleSummaryResponse> getUserFeed(Long targetMemberId, Long meId, Pageable pageable) {
        var sorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));

        Page<Article> page = articleRepository.findByAuthor_Id(targetMemberId, sorted);

        return mapToSummaryPage(meId, page);
    }

    private Page<ArticleSummaryResponse> mapToSummaryPage(Long meId, Page<Article> page) {
        var ids = page.map(Article::getId).getContent();
        if (ids.isEmpty()) {
            return new PageImpl<>(List.of(), page.getPageable(), page.getTotalElements());
        }

        // 비로그인 가드
        Set<Long> liked = (meId == null)
                ? Set.of()
                : new HashSet<>(likeRepository.findLikedArticleIdsByMember(meId, ids));

        Map<Long, List<Long>> tagMap = new HashMap<>();
        for (Object[] row : tagRepository.findTaggedMemberIdsByArticleIds(ids)) {
            Long articleId = (Long) row[0];
            Long memberId  = (Long) row[1];
            tagMap.computeIfAbsent(articleId, k -> new ArrayList<>()).add(memberId);
        }

        List<ArticleSummaryResponse> list = new ArrayList<>(page.getNumberOfElements());
        for (Article a : page.getContent()) {
            list.add(ArticleSummaryResponse.from(
                    a,
                    liked.contains(a.getId()),
                    tagMap.getOrDefault(a.getId(), List.of())
            ));
        }
        return new PageImpl<>(list, page.getPageable(), page.getTotalElements());
    }

    public ArticleDetailResponse getDetail(Long articleId, Long meId) {
        Article a = articleRepository.findById(articleId)
                .orElseThrow(() -> new EntityNotFoundException("ARTICLE_NOT_FOUND"));

        boolean likedByMe = (meId != null) &&
                likeRepository.existsByArticle_IdAndMember_Id(articleId, meId);

        return ArticleDetailResponse.from(a, likedByMe);
    }
}
