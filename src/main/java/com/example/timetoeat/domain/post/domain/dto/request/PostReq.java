package com.example.timetoeat.domain.post.domain.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "공지 요청")
public record PostReq(
        @Schema(description = "인원 수")
        int targetCount,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm", timezone = "Asia/Seoul")
        @Schema(
                description = "시간",
                example = "2025-08-20T15:30",
                pattern = "yyyy-MM-dd'T'HH:mm"
        )
        LocalDateTime meetingAt,
        @Schema(description = "만날 장소")
        String location,
        @Schema(description = "친구에게 전할 메세지")
        String message
) {
}
