package com.example.timetoeat.domain.referral.controller;

import com.example.timetoeat.domain.referral.dto.response.ReferralItemResponse;
import com.example.timetoeat.domain.referral.service.ReferralQueryService;
import com.example.timetoeat.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/referrals")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ReferralQueryController {

    private final ReferralQueryService queryService;

    // 내가 만든 추천 코드 목록(상태 포함)
    @GetMapping("/me")
    public ApiResponse<List<ReferralItemResponse>> myReferrals(
            @AuthenticationPrincipal(expression = "memberId") Long meId
    ) {
        return ApiResponse.success(queryService.getMyReferrals(meId));
    }
}
