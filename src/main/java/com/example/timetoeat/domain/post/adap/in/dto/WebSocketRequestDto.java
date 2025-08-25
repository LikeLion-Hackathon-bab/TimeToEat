package com.example.timetoeat.domain.post.adap.in.dto;

import com.example.timetoeat.domain.participation.adap.in.dto.ParticipationSocketDto;

import java.util.List;

public record WebSocketRequestDto(
        String announcementId,
        String location,
        String meetingAt,
        List<ParticipationSocketDto> participants,
        List<RecentMenuDto> recentMenu
) {
}
