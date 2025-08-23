package com.example.timetoeat.domain.member.controller;

import com.example.timetoeat.domain.member.dto.response.ProfileDetailResponse;
import com.example.timetoeat.domain.member.dto.response.ProfileResponse;
import com.example.timetoeat.domain.member.service.MemberProfileQueryService;
import com.example.timetoeat.global.common.ApiResponse;
import jakarta.validation.constraints.Positive;
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
public class MemberProfileQueryController {

    private final MemberProfileQueryService queryService;

    // 1) //profile -> { memberId, userName, profileImageUrl, bio, meetingCount }
    @GetMapping("/me/profile")
    public ApiResponse<ProfileResponse> getMyProfile(
            @AuthenticationPrincipal(expression = "memberId") Long meId) {

        return ApiResponse.success(queryService.getMyProfile(meId));
    }

    @GetMapping("/{memberId}/profile")
    public ApiResponse<ProfileResponse> getProfile(@PathVariable @Positive Long memberId) {

        return ApiResponse.success(queryService.getProfile(memberId));
    }

    // 2) /profile/detail -> { memberId, userName, profileImageUrl, bio, meetingCount, liked, disliked, allergy }
    @GetMapping("/me/profile/detail")
    public ApiResponse<ProfileDetailResponse> getMyProfileDetail(
            @AuthenticationPrincipal(expression = "memberId") Long meId) {
        return ApiResponse.success(queryService.getMyProfileDetail(meId));
    }

    @GetMapping("/{memberId}/profile/detail")
    public ApiResponse<ProfileDetailResponse> getProfileDetail(@PathVariable @Positive Long memberId) {
        return ApiResponse.success(queryService.getProfileDetail(memberId));
    }
}
