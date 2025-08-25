package com.example.timetoeat.domain.post.application.port.dto;

public record ParticipationInfo(
        Long authorId,
        String name,
        String profileImageUrl
) {
}
