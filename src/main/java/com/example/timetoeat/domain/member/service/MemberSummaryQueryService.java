// src/main/java/com/example/timetoeat/domain/member/service/MemberSummaryQueryService.java
package com.example.timetoeat.domain.member.service;

import com.example.timetoeat.domain.member.dto.response.MemberSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberSummaryQueryService {

    private final Clock clock;

    public MemberSummaryResponse getMySummary(Long memberId) {
        LocalDate today = LocalDate.now(clock); // 하루 기준 고정

        int seed = Objects.hash(
                memberId,
                today.getYear(),
                today.getMonthValue(),
                today.getDayOfMonth()
        );
        Random r = new Random(seed);

        int friends = 33 + r.nextInt(100);      // 33 ~ 132
        int meetingsDone = 12 + r.nextInt(20);  // 12 ~ 31
        int meetingsPlanned = 1 + r.nextInt(5); // 1 ~ 5

        return MemberSummaryResponse.of(friends, meetingsDone, meetingsPlanned);
    }
}
