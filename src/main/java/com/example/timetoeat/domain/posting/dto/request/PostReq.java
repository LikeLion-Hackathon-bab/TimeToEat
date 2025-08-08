package com.example.timetoeat.domain.posting.dto.request;

import java.time.LocalDateTime;

public record PostReq(
        int targetCount,
        LocalDateTime meetingAt,
        String location,
        String message
) {

}
