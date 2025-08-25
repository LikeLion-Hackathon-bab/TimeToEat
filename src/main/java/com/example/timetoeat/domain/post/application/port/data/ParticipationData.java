package com.example.timetoeat.domain.post.application.port.data;

public record ParticipationData(
        Long authorId,
        String name,
        String profileImageUrl
) {
}
