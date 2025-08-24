package com.example.timetoeat.domain.member.controller;

import com.example.timetoeat.domain.member.dto.request.UpdateProfileRequest;
import com.example.timetoeat.domain.member.service.MemberProfileCommandService;
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
public class MemberProfileCommandController {

    private final MemberProfileCommandService commandService;

    @PatchMapping("/me/profile")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateMyProfile(@AuthenticationPrincipal(expression = "memberId") Long meId,
                                @Valid @RequestBody UpdateProfileRequest req) {

        commandService.updateProfile(meId, req);
    }
}
