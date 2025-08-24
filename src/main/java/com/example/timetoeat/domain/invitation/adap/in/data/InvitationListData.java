package com.example.timetoeat.domain.invitation.adap.in.data;

import com.example.timetoeat.domain.invitation.domain.vo.InvitationId;

public record InvitationListData(
    InvitationId invitationId,
    String inviterName,
    String inviterProfileImageUrl
) {
}
