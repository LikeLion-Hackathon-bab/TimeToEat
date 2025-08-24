package com.example.timetoeat.domain.coupon.controller;

import com.example.timetoeat.domain.coupon.dto.response.CouponResponse;
import com.example.timetoeat.domain.coupon.service.CouponCommandService;
import com.example.timetoeat.domain.coupon.service.CouponQueryService;
import com.example.timetoeat.global.common.ApiResponse;
import com.example.timetoeat.domain.coupon.model.CouponStatus;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/coupons")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class CouponController {

    private final CouponQueryService couponQueryService;
    private final CouponCommandService couponCommandService;

    // 쿠폰함 조회: status = <UNUSED / USED>
    @GetMapping("/me")
    public ApiResponse<List<CouponResponse>> getMyCoupons(
            @AuthenticationPrincipal(expression = "memberId") Long meId,
            @RequestParam(required = false) CouponStatus status
    ) {
        return ApiResponse.success(couponQueryService.getMyCoupons(meId, status));
    }


    // 쿠폰 사용 처리
    @PostMapping("/{couponId}/use")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void use(
            @AuthenticationPrincipal(expression = "memberId") Long meId,
            @PathVariable @Positive Long couponId
    ) {
        couponCommandService.use(meId, couponId);
    }
}
