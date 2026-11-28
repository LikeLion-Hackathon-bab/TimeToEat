package com.example.timetoeat.domain.recommendation.controller;

import com.example.timetoeat.domain.recommendation.dto.MenuRecommendationResponse;
import com.example.timetoeat.domain.recommendation.service.MenuRecommendationService;
import com.example.timetoeat.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** 개인화 AI 메뉴 추천(SCR-24, AI-05). GET /api/v1/recommendations/me?topK=5 */
@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class MenuRecommendationController {

    private final MenuRecommendationService service;

    @GetMapping("/me")
    public ApiResponse<MenuRecommendationResponse> me(
            @AuthenticationPrincipal(expression = "memberId") Long meId,
            @RequestParam(defaultValue = "5") int topK
    ) {
        return ApiResponse.success(service.recommendForMe(meId, topK));
    }
}
