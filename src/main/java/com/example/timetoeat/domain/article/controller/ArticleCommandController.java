/*
package com.example.timetoeat.domain.article.controller;

import com.example.timetoeat.domain.article.dto.request.CreateArticleRequest;
import com.example.timetoeat.domain.article.dto.request.CreateCommentRequest;
import com.example.timetoeat.domain.article.dto.response.ArticleLikeToggleResponse;
import com.example.timetoeat.domain.article.service.ArticleCommandService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

@Validated
@RestController
@RequestMapping("/api/v1/articles")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ArticleCommandController {

    private final ArticleCommandService articleCommandService;

    // 게시글 업로드
    @PostMapping
    public ResponseEntity<Void> createArticle(
            @Valid @RequestBody CreateArticleRequest createArticleRequest,
            @AuthenticationPrincipal(expression = "memberId") Long meId,
            UriComponentsBuilder uriComponentsBuilder) {

        Long id = articleCommandService.createArticle(createArticleRequest, meId);

        return ResponseEntity.created(uriComponentsBuilder.path("/api/v1/articles/{id}").buildAndExpand(id).toUri()).build();
    }

    // 게시글 삭제 (작성자만)
    @DeleteMapping("/{articleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteArticle(
            @PathVariable @Positive Long articleId,
            @AuthenticationPrincipal(expression = "memberId") Long meId
    ) {

        articleCommandService.deleteArticle(articleId, meId);
    }

    // '좋아요' 토글
    @PostMapping("/{articleId}/like")
    public ArticleLikeToggleResponse toggleLike(
            @PathVariable @Positive Long articleId,
            @AuthenticationPrincipal(expression = "memberId") Long meId
    ) {

        return articleCommandService.toggleLike(articleId, meId);
    }

    // 댓글 / 댓글 작성
    @PostMapping("/{articleId}/comments")
    public ResponseEntity<Long> addComment(
            @PathVariable @Positive Long articleId,
            @Valid @RequestBody CreateCommentRequest createCommentRequest,
            @AuthenticationPrincipal(expression = "memberId") Long meId
    ) {

        Long id = articleCommandService.addComment(articleId, meId, createCommentRequest);

        return ResponseEntity.status(HttpStatus.CREATED).body(id);
    }

    // 댓글 삭제 (글 소유자: 모든 댓글 가능, 댓글 작성자: 본인 댓글만 가능)
    @DeleteMapping("/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(
            @PathVariable @Positive Long commentId,
            @AuthenticationPrincipal(expression = "memberId") Long meId
    ) {
        articleCommandService.deleteComment(commentId, meId);
    }
}
*/
