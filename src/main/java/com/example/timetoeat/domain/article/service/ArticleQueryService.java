package com.example.timetoeat.domain.article.service;

import com.example.timetoeat.domain.article.dto.response.ArticleDetailResponse;
import com.example.timetoeat.domain.article.dto.response.ArticleSummaryResponse;
import com.example.timetoeat.domain.article.dto.response.CommentResponse;
import com.example.timetoeat.domain.article.entity.Article;
import com.example.timetoeat.domain.article.entity.ArticleComment;
import com.example.timetoeat.domain.article.repository.ArticleCommentRepository;
import com.example.timetoeat.domain.article.repository.ArticleLikeRepository;
import com.example.timetoeat.domain.article.repository.ArticleRepository;
import com.example.timetoeat.domain.article.repository.ArticleTagRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
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
    private final ArticleCommentRepository articleCommentRepository;

    // 로그인 필수
    private static Long requireLogin(Long meId) {
        if (meId == null) throw new AccessDeniedException("UNAUTHENTICATED");
        return meId;
    }

    public Page<ArticleSummaryResponse> getHomeFeed(Long meId, Pageable pageable, LocalDateTime now) {

        Long loginId = requireLogin(meId);

        var threshold = now.minusHours(24);
        var sorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));

        Page<Article> page = articleRepository.findByCreatedAtGreaterThanEqual(threshold, sorted);

        return mapToSummaryPage(loginId, page);
    }

    public Page<ArticleSummaryResponse> getUserFeed(Long targetMemberId, Long meId, Pageable pageable) {

        Long loginId = requireLogin(meId);

        var sorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));

        Page<Article> page = articleRepository.findByAuthor_Id(targetMemberId, sorted);

        return mapToSummaryPage(loginId, page);
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

        Long loginId = requireLogin(meId);

        Article a = articleRepository.findById(articleId)
                .orElseThrow(() -> new EntityNotFoundException("ARTICLE_NOT_FOUND"));

        boolean likedByMe = likeRepository.existsByArticle_IdAndMember_Id(articleId, loginId);

        return ArticleDetailResponse.from(a, likedByMe);
    }

    public List<CommentResponse> getCommentsFlat(Long articleId) {

        if (!articleRepository.existsById(articleId)) {
            throw new EntityNotFoundException("ARTICLE_NOT_FOUND");
        }

        // 루트 댓글 한 번에
        List<ArticleComment> roots =
                articleCommentRepository.findByArticle_IdAndParentCommentIsNullOrderByIdAsc(articleId);
        if (roots.isEmpty()) return List.of();

        // 루트들의 id 모아 대댓글을 한 번에
        List<Long> parentIds = roots.stream().map(ArticleComment::getId).toList();
        List<ArticleComment> replies =
                articleCommentRepository.findByParentComment_IdInOrderByIdAsc(parentIds);

        // parentId -> 대댓글들 맵 구성
        Map<Long, List<ArticleComment>> repliesByParent = new HashMap<>();
        for (ArticleComment r : replies) {
            Long pid = r.getParentComment().getId();
            repliesByParent.computeIfAbsent(pid, k -> new ArrayList<>()).add(r);
        }

        List<CommentResponse> result = new ArrayList<>(roots.size() + replies.size());
        for (ArticleComment root : roots) {
            result.add(CommentResponse.from(root));
            List<ArticleComment> children = repliesByParent.get(root.getId());
            if (children != null) {
                for (ArticleComment child : children) {
                    result.add(CommentResponse.from(child));
                }
            }
        }
        return result;
    }
}
