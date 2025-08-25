package com.example.timetoeat.domain.member.controller;

import com.example.timetoeat.domain.member.dto.response.MemberSummaryResponse;
import com.example.timetoeat.domain.member.service.MemberSummaryQueryService;
import com.example.timetoeat.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class MemberSummaryQueryController {

    private final MemberSummaryQueryService service;

    // 내 정보(친구 수/ 완료 약속 수 / 예정 약속 수)
    @GetMapping("/me/summary")
    public ApiResponse<MemberSummaryResponse> getMySummary(
            @AuthenticationPrincipal(expression = "memberId") Long meId
    ) {
        return ApiResponse.success(service.getMySummary(meId));
    }
}
