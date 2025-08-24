package com.example.timetoeat.domain.post.application.port.data;

import java.time.LocalDateTime;
import java.util.List;

public record PostData(
        Long postId,
        String authorName,
        String authorProfileImageUrl,
        String message,
        LocalDateTime meetingAt,
        String location,
        List<ParticipationData> participants,
        int targetCount
){
}
