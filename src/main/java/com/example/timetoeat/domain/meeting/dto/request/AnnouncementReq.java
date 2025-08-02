package com.example.timetoeat.domain.meeting.dto.request;

import com.example.timetoeat.domain.meeting.adapter.out.persistence.AnnouncementStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record AnnouncementReq(
        LocalDate date,
        LocalTime time,
        String location,
        int memberLimit,
        LocalDateTime expireAt,
        AnnouncementStatus status
) {

}
