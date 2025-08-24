package com.example.timetoeat.domain.referral.controller;

import com.example.timetoeat.domain.referral.service.ReferralCommandService;
import com.example.timetoeat.global.common.ApiResponse;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/v1/referrals")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ReferralCommandController {

    private final ReferralCommandService commandService;

    // 추천 코드 생성(링크 포함 반환)
    @PostMapping
    public ApiResponse<?> create(@AuthenticationPrincipal(expression = "memberId") Long meId) {
        return ApiResponse.success(commandService.create(meId));
    }

    // 추천 코드 수령(가입 완료 후, 로그인 상태에서 호출)
    @PostMapping("/redeem")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void redeem(
            @AuthenticationPrincipal(expression = "memberId") Long meId,
            @RequestParam @NotBlank String code
    ) {
        commandService.redeem(meId, code);
    }
}
