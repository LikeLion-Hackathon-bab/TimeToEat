package com.example.timetoeat.domain.invitation.adap.in.web;

import com.example.timetoeat.domain.invitation.adap.in.dto.InvitationListResponse;
import com.example.timetoeat.domain.invitation.application.port.in.usecase.GetInvitationUseCase;
import com.example.timetoeat.domain.invitation.application.port.in.usecase.InvitationUseCase;
import com.example.timetoeat.domain.post.domain.vo.member.MemberId;
import com.example.timetoeat.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/invitations")
@RequiredArgsConstructor
@Tag(name = "invitation API")
public class InvitationQueryController {

    private final GetInvitationUseCase getInvitationUseCase;

    @GetMapping
    public ApiResponse<List<InvitationListResponse>> getInvitationList(
        @AuthenticationPrincipal(expression = "memberId") Long memberId
    ) {
        if (memberId == null) {
            throw new IllegalStateException("로그인이 필요한 서비스입니다.");
        }
        List<InvitationListResponse> invitationList = getInvitationUseCase.getInvitationList(new MemberId(memberId));
        return ApiResponse.success(invitationList);
    }
}
