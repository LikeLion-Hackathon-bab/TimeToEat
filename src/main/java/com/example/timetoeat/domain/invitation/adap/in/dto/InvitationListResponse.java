package com.example.timetoeat.domain.invitation.adap.in.dto;

public record InvitationListResponse(
    Long invitationId,
    String inviterName,
    String inviterProfileImageUrl
) {
}
