package com.example.timetoeat.domain.posting.api.web.participation;

import com.example.timetoeat.domain.posting.core.gateway.service.in.usecase.participation.ParticipationUseCase;
import com.example.timetoeat.domain.posting.domain.vo.MemberId;
import com.example.timetoeat.domain.posting.domain.vo.PostId;
import com.example.timetoeat.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/participate")
@RequiredArgsConstructor
@Tag(name = "공지 참여 API")
public class ParticipationController {
    private final ParticipationUseCase participationUseCase;

    @PostMapping("/{postId}")
    @Operation(summary = "공지 참여 API")
    public ApiResponse<Object> participate(
            @AuthenticationPrincipal(expression = "memberId") Long memberId,
            @PathVariable Long postId
    )
    {
        if (memberId == null || postId == null) {
            throw new IllegalStateException("공지 참여 입력값이 유효하지 않습니다");
        }
        participationUseCase.apply(new MemberId(memberId), new PostId(postId));
        return ApiResponse.success("참가 신청이 완료되었습니다.");
    }
}
