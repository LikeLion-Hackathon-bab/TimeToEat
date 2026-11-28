package com.example.timetoeat.domain.post.application.port.data;

import java.time.LocalDateTime;
import java.util.List;

public record PostData(
        Long postId,
        LocalDateTime createdAt,
        Long authorId,
        String authorName,
        String authorProfileImageUrl,
        String message,
        LocalDateTime meetingAt,
        String location,
        List<ParticipationData> participants,
        int targetCount,
        /** 모집 마감 시각(KST). 다시 모집(F46)하면 createdAt+2시간과 달라진다. */
        LocalDateTime expiredAt
){
}
