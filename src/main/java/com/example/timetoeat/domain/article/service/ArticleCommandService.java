package com.example.timetoeat.domain.article.service;

import com.example.timetoeat.domain.article.dto.request.CreateArticleRequest;
import com.example.timetoeat.domain.article.dto.request.CreateCommentRequest;
import com.example.timetoeat.domain.article.dto.response.ArticleLikeToggleResponse;
import com.example.timetoeat.domain.article.entity.*;
import com.example.timetoeat.domain.article.repository.*;
import com.example.timetoeat.domain.member.entity.MemberEntity;
import com.example.timetoeat.infra.ai.AiGateway;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.OffsetDateTime;
import java.util.HashSet;

@Service
@RequiredArgsConstructor
@Transactional
public class ArticleCommandService {

    private final ArticleRepository articleRepository;
    private final ArticleLikeRepository likeRepository;
    private final ArticleCommentRepository commentRepository;
    private final ArticleTagRepository tagRepository;
    private final EntityManager entityManager;
    private final AiGateway aiGateway;

    @Value("${app.ai.callback-base}")
    private String callbackBase;

    private final Clock clock;

    // 게시글 작성
    public Long createArticle(CreateArticleRequest req, Long authorId) {
        MemberEntity authorRef = entityManager.getReference(MemberEntity.class, authorId);

        LocalDateTime nowKst = LocalDateTime.now(clock);
        var resolvedDate = req.isCamera() ? nowKst.toLocalDate() : req.getMealDate();
        var resolvedTime = req.isCamera() ? nowKst.toLocalTime() : req.getMealTime();

        Article article = req.toEntity(authorRef, resolvedDate, resolvedTime);
        articleRepository.save(article);

        // 태그 저장 (자기 자신 제거 + 중복 제거)
        var ids = new HashSet<>(req.getTaggedMemberIds());
        ids.remove(authorId);
        for (Long taggedId : ids) {
            MemberEntity taggedRef = entityManager.getReference(MemberEntity.class, taggedId);
            tagRepository.save(ArticleTag.of(taggedRef, article));
        }

        Long id = article.getId();

        OffsetDateTime mealAtKst = LocalDateTime.of(resolvedDate, resolvedTime)
                .atZone(ZoneId.of("Asia/Seoul"))
                .toOffsetDateTime();

        String callbackUrl = UriComponentsBuilder
                .fromHttpUrl(callbackBase)
                .path("/api/v1/articles/{id}/ai/inference")
                .buildAndExpand(id)
                .toUriString();

        aiGateway.requestInference(id, authorId, article.getImageUrl(), mealAtKst, callbackUrl);

        return id;
    }

    // 게시글 삭제 (작성자만)
    public void deleteArticle(Long articleId, Long requesterId) {
        int deleted = articleRepository.deleteByIdAndAuthorId(articleId, requesterId);
        if (deleted > 0) return;  // 바로 성공

        // 아무 것도 안 지워졌으면 이유 판별(403 / 404)
        if (articleRepository.existsById(articleId)) {
            throw new AccessDeniedException("NO_PERMISSION");
        }
        throw new EntityNotFoundException("ARTICLE_NOT_FOUND");
    }

    // '좋아요' 토글
    public ArticleLikeToggleResponse toggleLike(Long articleId, Long memberId) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new EntityNotFoundException("ARTICLE_NOT_FOUND"));

        var existing = likeRepository.findByArticle_IdAndMember_Id(articleId, memberId);
        if (existing.isPresent()) {
            // 이미 '좋아요' -> 취소
            likeRepository.delete(existing.get());
            article.decreaseLike();
            return ArticleLikeToggleResponse.of(false, article.getLikeCount());
        } else {
            try {
                MemberEntity memberRef = entityManager.getReference(MemberEntity.class, memberId);
                likeRepository.save(ArticleLike.builder()
                        .member(memberRef)
                        .article(article)
                        .build());
                article.increaseLike();
                return ArticleLikeToggleResponse.of(true, article.getLikeCount());
            } catch (DataIntegrityViolationException dup) {
                entityManager.refresh(article);
                return ArticleLikeToggleResponse.of(true, article.getLikeCount());
            }
        }
    }

    // 댓글/대댓글 작성
    public Long addComment(Long articleId, Long memberId, CreateCommentRequest req) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new EntityNotFoundException("ARTICLE_NOT_FOUND"));
        MemberEntity authorRef = entityManager.getReference(MemberEntity.class, memberId);

        ArticleComment parent = null;
        if (req.getParentCommentId() != null) {
            parent = commentRepository.findById(req.getParentCommentId())
                    .orElseThrow(() -> new EntityNotFoundException("PARENT_COMMENT_NOT_FOUND"));
            // 부모 댓글이 같은 글인지 확인
            if (!parent.getArticle().getId().equals(article.getId())) {
                throw new AccessDeniedException("PARENT_COMMENT_NOT_IN_ARTICLE");
            }
        }

        ArticleComment saved = commentRepository.save(
                req.toEntity(authorRef, article, parent)
        );
        article.increaseComment();
        return saved.getId();
    }

    // 댓글 삭제 (글 소유자: 모든 댓글 / 댓글 작성자: 본인 댓글만)
    public void deleteComment(Long commentId, Long requesterId) {
        ArticleComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("COMMENT_NOT_FOUND"));

        boolean isArticleOwner = comment.getArticle().isAuthoredBy(requesterId);
        boolean isCommentAuthor = comment.isWrittenBy(requesterId);
        if (!isArticleOwner && !isCommentAuthor) throw new AccessDeniedException("NO_PERMISSION");

        int delta = 1;
        if (comment.getParentComment() == null) {
            long replies = commentRepository.countByParentComment_Id(comment.getId());
            delta += (int) replies;
        }
        commentRepository.delete(comment);
        comment.getArticle().decreaseCommentBy(delta);
    }
}
