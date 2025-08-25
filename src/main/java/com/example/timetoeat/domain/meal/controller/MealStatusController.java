package com.example.timetoeat.domain.meal.controller;

import com.example.timetoeat.domain.meal.dto.request.UpdateMealStatusRequest;
import com.example.timetoeat.domain.meal.dto.response.MealStatusResponse;
import com.example.timetoeat.domain.meal.service.MealStatusService;
import com.example.timetoeat.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class MealStatusController {

    private final MealStatusService service;

    // 내 식사 상태 조회
    @GetMapping("/me/meal-status")
    public ApiResponse<MealStatusResponse> getMyStatus(
            @AuthenticationPrincipal(expression = "memberId") Long meId) {
        return ApiResponse.success(service.getMyStatus(meId));
    }

    // 토글(ON=ATE_NOW / OFF=SET_OFF)
    @PatchMapping("/me/meal-status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void update(
            @AuthenticationPrincipal(expression = "memberId") Long meId,
            @Valid @RequestBody UpdateMealStatusRequest req) {
        service.update(meId, req.getAction());
    }
}
