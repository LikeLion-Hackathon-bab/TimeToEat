package com.example.timetoeat.domain.posting.adapter.in.web.participation;

import com.example.timetoeat.domain.posting.application.port.in.usecase.ParticipationUseCase;
import com.example.timetoeat.domain.posting.domain.vo.MemberId;
import com.example.timetoeat.domain.posting.domain.vo.PostId;
import com.example.timetoeat.global.auth.model.CustomOauth2User;
import com.example.timetoeat.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/participate")
@RequiredArgsConstructor
public class ParticipationController {
    private final ParticipationUseCase participationUseCase;

    @PostMapping("/{postId}")
    public ApiResponse<Object> participate(
            @AuthenticationPrincipal CustomOauth2User customOauth2User,
            @PathVariable Long postId
    )
    {
        participationUseCase.apply(new MemberId(customOauth2User.getMemberId()), new PostId(postId));
        return ApiResponse.success("참가 신청이 완료되었습니다.");
    }
}
