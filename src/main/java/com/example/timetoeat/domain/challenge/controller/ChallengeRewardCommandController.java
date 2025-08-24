package com.example.timetoeat.domain.challenge.controller;

import com.example.timetoeat.domain.challenge.entity.ChallengeRewardHistory.RewardType;
import com.example.timetoeat.domain.challenge.service.ChallengeRewardCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/v1/challenges")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ChallengeRewardCommandController {

    private final ChallengeRewardCommandService service;

    // 보상 수령 (주간/월간)
    @PostMapping("/me/reward")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void claim(
            @AuthenticationPrincipal(expression = "memberId") Long meId,
            @RequestParam RewardType type // WEEK | MONTH
    ) {
        service.claim(meId, type);
    }
}
