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

@RestController
@RequestMapping("/api/v1/invitations")
@RequiredArgsConstructor
@Tag(name = "invitation API")
public class InvitationCommandController {
    private final InvitationUseCase invitationUseCase;

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
}
