package com.example.timetoeat.domain.article.controller;

import com.example.timetoeat.domain.article.dto.response.ArticleDetailResponse;
import com.example.timetoeat.domain.article.dto.response.ArticleSummaryResponse;
import com.example.timetoeat.domain.article.dto.response.CommentResponse;
import com.example.timetoeat.domain.article.service.ArticleQueryService;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/articles")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ArticleQueryController {

    private final ArticleQueryService articleQueryService;
    private final Clock clock;

    // 홈 피드 조회: 24시간 이내 + 최신순
    @GetMapping("/home")
    public Page<ArticleSummaryResponse> getHomeFeed(
            @AuthenticationPrincipal(expression = "memberId") Long meId,
            @ParameterObject @PageableDefault(size = 10) Pageable pageable
    ) {
        LocalDateTime now = LocalDateTime.now(clock);
        return articleQueryService.getHomeFeed(meId, pageable, now);
    }

    // 특정 사용자의 프로필(밥그릇): 전체 + 최신순
    @GetMapping("/by-author/{authorId}")
    public Page<ArticleSummaryResponse> getUserFeed(
            @AuthenticationPrincipal(expression = "memberId") Long meId,
            @PathVariable @Positive Long authorId,
            @ParameterObject @PageableDefault(size = 10) Pageable pageable
    ) {

        return articleQueryService.getUserFeed(authorId, meId, pageable);
    }

    // 게시글 단건 조회
    @GetMapping("/{articleId}")
    public ArticleDetailResponse getDetail(
            @AuthenticationPrincipal(expression = "memberId") Long meId,
            @PathVariable @Positive Long articleId
    ) {

        return articleQueryService.getDetail(articleId, meId);
    }

    // 단건 조회에서 댓글+대댓글 일괄 조회
    @GetMapping("/{articleId}/comments")
    public List<CommentResponse> getComments(
            @PathVariable @Positive Long articleId
    ) {

        return articleQueryService.getCommentsFlat(articleId);
    }
}
