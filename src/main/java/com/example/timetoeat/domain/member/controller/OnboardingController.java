package com.example.timetoeat.domain.member.controller;

import com.example.timetoeat.domain.member.dto.request.ProfileOnboardingRequest;
import com.example.timetoeat.domain.member.service.OnboardingFacadeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class OnboardingController {

    private final OnboardingFacadeService onboardingFacadeService;

    // 프로필 + 음식(선호/비선호/알러지) 한 번에 저장
    @PostMapping("/onboarding")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void onboard(
            @AuthenticationPrincipal(expression = "memberId") Long meId,
            @Valid @RequestBody ProfileOnboardingRequest req) {

        onboardingFacadeService.onboard(meId, req);
    }
}
