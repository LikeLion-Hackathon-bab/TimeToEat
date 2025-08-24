package com.example.timetoeat.domain.invitation.application.port.in.command;

import com.example.timetoeat.domain.post.domain.vo.member.MemberId;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SendInvitationCommand(
    @NotNull(message = "초대할 회원의 ID는 필수입니다.")
    MemberId inviteeId,
    @NotBlank(message = "초대 메시지를 입력해야 합니다.")
    @Size(max = 100, message = "초대 메시지는 100자 이내여야 합니다.")
    String message
) {}
