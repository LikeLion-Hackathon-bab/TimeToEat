package com.example.timetoeat.domain.post.application.port.dto.response;

import com.example.timetoeat.domain.post.application.port.dto.AuthorInfo;
import com.example.timetoeat.domain.post.application.port.dto.ParticipationInfo;

import java.time.LocalDateTime;
import java.util.List;

public record PostRes(
        Long postId,
        LocalDateTime createdAt,
        AuthorInfo author,
        String message,
        LocalDateTime meetingAt,
        String location,
        List<ParticipationInfo> participants,
        int targetCount
){
}
