package com.example.timetoeat.domain.invitation.adap.in.dto;

import com.example.timetoeat.domain.participation.adap.in.dto.ParticipationSocketDto;
import com.example.timetoeat.domain.post.adap.in.dto.RecentMenuDto;

import java.util.List;


public record InvitationSocketRequestDto(
        String invitationId,
        List<ParticipationSocketDto> participants,
        List<RecentMenuDto> recentMenu
) {
}
