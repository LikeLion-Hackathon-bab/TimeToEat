package com.example.timetoeat.domain.invitation.adap.in.web;

import com.example.timetoeat.domain.invitation.application.port.in.command.SendInvitationCommand;
import com.example.timetoeat.domain.invitation.application.port.in.usecase.InvitationUseCase;
import com.example.timetoeat.domain.invitation.domain.vo.InvitationId;
import com.example.timetoeat.domain.invitation.adap.in.dto.InvitationRequest;
import com.example.timetoeat.domain.post.domain.vo.member.MemberId;
import com.example.timetoeat.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/v1/invitations")
@RequiredArgsConstructor
@Tag(name = "invitation-controller")
public class InvitationCommandController {
    private final InvitationUseCase invitationUseCase;
    private final com.example.timetoeat.domain.invitation.application.service.InvitationRoomService rooms;
    private final com.example.timetoeat.domain.plans.meeting.RoomLeaveService leaving;

    @PostMapping("/{id}/enter")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void enter(@AuthenticationPrincipal(expression = "memberId") Long viewer, @PathVariable long id) {
        rooms.enter(viewer, id);
    }

    @PostMapping("/send")
    public ApiResponse<Object> sendInvitation(
        @AuthenticationPrincipal(expression = "memberId") Long memberId,
        @RequestBody @Valid InvitationRequest request)
    {
        if (memberId == null) {
            throw new IllegalStateException("로그인이 필요한 서비스입니다.");
        }
        SendInvitationCommand command = new SendInvitationCommand(
                new MemberId(request.inviteeId().getId()),
                request.message()
        );
        invitationUseCase.sendInvitation(new MemberId(memberId), command);
        return ApiResponse.success("success");
    }

    @PostMapping("/{invitationId}/accept")
    public ApiResponse<Object> acceptInvitation(
            @AuthenticationPrincipal(expression = "memberId") Long memberId,
            @PathVariable Long invitationId) {
        if (memberId == null) {
            throw new IllegalStateException("로그인이 필요한 서비스입니다.");
        }
        String result = invitationUseCase.acceptInvitation(new MemberId(memberId), new InvitationId(invitationId));
        return ApiResponse.success(result);
    }

    @PostMapping("/{invitationId}/reject")
    public ApiResponse<Object> rejectInvitation(
            @AuthenticationPrincipal(expression = "memberId") Long memberId,
            @PathVariable Long invitationId) {
        if (memberId == null) {
            throw new IllegalStateException("로그인이 필요한 서비스입니다.");
        }
        invitationUseCase.rejectInvitation(new MemberId(memberId), new InvitationId(invitationId));
        return ApiResponse.success("success");
    }

    /** 수락된 초대의 대기실에서 나가기(F46). 보낸 사람·받은 사람 모두 가능. 계약: qa-cloud/contracts/participation-cancel.md */
    @PostMapping("/{invitationId}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@AuthenticationPrincipal(expression = "memberId") Long memberId, @PathVariable long invitationId) {
        leaving.cancelInvitation(memberId, invitationId);
    }
}
