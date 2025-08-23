package com.example.timetoeat.domain.article.controller;

import com.example.timetoeat.domain.article.dto.response.RecentMealsResponse;
import com.example.timetoeat.domain.article.service.ArticleMealQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/articles/meals")
@PreAuthorize("isAuthenticated()")
public class ArticleMealQueryController {

    private final ArticleMealQueryService queryService;

    // 최근 72시간(3일) 동안 먹은 음식: 내가 쓴 글 + 내가 태그된 글 모두 포함
    @GetMapping("/recent")
    public RecentMealsResponse myRecentMeals(@AuthenticationPrincipal(expression = "memberId") Long meId) {
        return queryService.getMyRecentMeals(meId);
    }
}
