package com.example.timetoeat.domain.post.application.port.dto;

public record AuthorInfo(
        Long authorId,
        String name,
        String profileImageUrl
) {
}
