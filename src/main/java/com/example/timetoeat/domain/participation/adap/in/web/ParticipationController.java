package com.example.timetoeat.domain.participation.adap.in.web;

import com.example.timetoeat.domain.participation.application.port.in.usecase.ParticipationUseCase;
import com.example.timetoeat.domain.post.domain.vo.member.MemberId;
import com.example.timetoeat.domain.post.domain.vo.post.PostId;
import com.example.timetoeat.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/participate")
@RequiredArgsConstructor
@Tag(name = "공지 참여 API")
public class ParticipationController {
    private final ParticipationUseCase participationUseCase;
    private final com.example.timetoeat.domain.plans.meeting.RoomLeaveService rooms;

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

    /** 참여 취소·대기실 나가기(F46). 계약: qa-cloud/contracts/participation-cancel.md */
    @DeleteMapping("/{postId}")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    @Operation(summary = "공지 참여 취소(대기실 나가기) API")
    public void cancel(@AuthenticationPrincipal(expression = "memberId") Long memberId, @PathVariable long postId) {
        rooms.leaveAnnouncement(memberId, postId);
    }
}
