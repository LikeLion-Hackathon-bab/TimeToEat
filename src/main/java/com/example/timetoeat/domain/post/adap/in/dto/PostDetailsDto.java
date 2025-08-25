package com.example.timetoeat.domain.post.adap.in.dto;

import java.time.LocalDateTime;

public record PostDetailsDto(
        String location,
        LocalDateTime meetingAt
) {
}
