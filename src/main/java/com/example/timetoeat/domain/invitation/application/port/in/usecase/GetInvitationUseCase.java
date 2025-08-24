package com.example.timetoeat.domain.invitation.application.port.in.usecase;

import com.example.timetoeat.domain.invitation.adap.in.dto.InvitationListResponse;
import com.example.timetoeat.domain.post.domain.vo.member.MemberId;

import java.util.List;

public interface GetInvitationUseCase {
    List<InvitationListResponse> getInvitationList(MemberId inviteeId);
}
