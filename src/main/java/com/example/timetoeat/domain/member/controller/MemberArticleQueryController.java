package com.example.timetoeat.domain.member.controller;

import com.example.timetoeat.domain.article.dto.response.ArticleSummaryResponse;
import com.example.timetoeat.domain.article.service.ArticleQueryService;
import com.example.timetoeat.global.common.ApiResponse;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class MemberArticleQueryController {  // 확장성 + 도메인 경계 분리를 위해 엔드포인트 추가

    private final ArticleQueryService articleQueryService;

    // 내 게시글 목록
    @GetMapping("/me/articles")
    public ApiResponse<Page<ArticleSummaryResponse>> getMyArticles(
            @AuthenticationPrincipal(expression = "memberId") Long meId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {

        return ApiResponse.success(articleQueryService.getUserFeed(meId, meId, pageable));
    }

    // 특정 사용자 게시글 목록
    @GetMapping("/{memberId}/articles")
    public ApiResponse<Page<ArticleSummaryResponse>> getMemberArticles(
            @PathVariable @Positive Long memberId,
            @AuthenticationPrincipal(expression = "memberId") Long meId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {

        return ApiResponse.success(articleQueryService.getUserFeed(memberId, meId, pageable));
    }
}
