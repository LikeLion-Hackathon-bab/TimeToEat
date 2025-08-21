package com.example.timetoeat.domain.preference.controller;

import com.example.timetoeat.domain.preference.dto.request.OnboardingPreferenceRequest;
import com.example.timetoeat.domain.preference.service.PreferenceCommandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/preferences")
@PreAuthorize("isAuthenticated()")
public class PreferenceCommandController {

    private final PreferenceCommandService commandService;

    @PostMapping("/onboarding")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void saveOnboarding(
            @AuthenticationPrincipal(expression = "memberId") Long meId,
            @Valid @RequestBody OnboardingPreferenceRequest req
    ) {
        commandService.saveOnboarding(meId, req);
    }
}
