package com.example.timetoeat.domain.preference.controller;

import com.example.timetoeat.domain.preference.dto.response.PreferenceMetaResponse;
import com.example.timetoeat.domain.preference.dto.response.PreferenceSummaryResponse;
import com.example.timetoeat.domain.preference.service.PreferenceQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/preferences")
@PreAuthorize("isAuthenticated()")
public class PreferenceQueryController {

    private final PreferenceQueryService queryService;

    @GetMapping("/me")
    public PreferenceSummaryResponse myPreferences(
            @AuthenticationPrincipal(expression = "memberId") Long meId
    ) {
        return queryService.getMySummary(meId);
    }

    @GetMapping("/me/meta")
    public PreferenceMetaResponse myPreferenceMeta(
            @AuthenticationPrincipal(expression = "memberId") Long meId
    ) {
        return queryService.getMyMeta(meId);
    }
}
