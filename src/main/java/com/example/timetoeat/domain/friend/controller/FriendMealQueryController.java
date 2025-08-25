package com.example.timetoeat.domain.friend.controller;

import com.example.timetoeat.domain.friend.dto.response.FriendMealItemResponse;
import com.example.timetoeat.domain.friend.service.FriendMealQueryService;
import com.example.timetoeat.global.common.ApiResponse;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/friends")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class FriendMealQueryController {

    private final FriendMealQueryService service;

    // filter = ALL | HUNGRY(밥 안 먹음) | NOT_HUNGRY(밥 먹음)
    @GetMapping("/me/meals")
    public ApiResponse<List<FriendMealItemResponse>> getList(
            @AuthenticationPrincipal(expression = "memberId") Long meId,
            @RequestParam(defaultValue = "ALL")
            @Pattern(regexp = "ALL|HUNGRY|NOT_HUNGRY") String filter
    ) {
        return ApiResponse.success(service.getDemoList(meId, filter));
    }
}
