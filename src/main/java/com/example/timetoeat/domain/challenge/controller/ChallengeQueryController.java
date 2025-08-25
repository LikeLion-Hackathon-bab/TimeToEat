package com.example.timetoeat.domain.challenge.controller;

import com.example.timetoeat.domain.challenge.dto.response.ChallengeStatusResponse;
import com.example.timetoeat.domain.challenge.service.ChallengeQueryService;
import com.example.timetoeat.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/v1/challenges")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ChallengeQueryController {

    private final ChallengeQueryService challengeQueryService;

    // 주간/월간 챌린지 진행도 조회
    @GetMapping("/me")
    public ApiResponse<ChallengeStatusResponse> getMyStatus(
            @AuthenticationPrincipal(expression = "memberId") Long meId
    ) {
        return ApiResponse.success(challengeQueryService.getMyStatus(meId));
    }
}
