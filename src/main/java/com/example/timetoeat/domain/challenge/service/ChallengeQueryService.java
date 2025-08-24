package com.example.timetoeat.domain.challenge.service;

import com.example.timetoeat.domain.article.repository.ArticleRepository;
import com.example.timetoeat.domain.challenge.dto.response.ChallengeStatusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChallengeQueryService {

    @Qualifier("kstClock")
    private final Clock clock;
    private final ArticleRepository articleRepository;

    // 오늘 포함 ‘해당 날짜’에 게시글 1개 이상 올렸는지
    private boolean postedOn(Long memberId, LocalDate date) {
        ZoneId zone = clock.getZone();
        LocalDateTime start = date.atStartOfDay(zone).toLocalDateTime();
        LocalDateTime end   = start.plusDays(1);

        return articleRepository.countByAuthor_IdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(memberId, start, end) > 0;
    }

    // 오늘부터 과거로 연속 게시 일수(스트릭) 계산
    private int currentStreak(Long memberId) {
        LocalDate d = LocalDate.now(clock);

        int streak = 0;
        while (postedOn(memberId, d)) {
            streak++;
            d = d.minusDays(1);
            if (streak > 400) break;  // 안전장치
        }

        return streak;
    }

    public ChallengeStatusResponse getMyStatus(Long memberId) {
        LocalDate today = LocalDate.now(clock);
        YearMonth ym = YearMonth.from(today);
        int daysInMonth = ym.lengthOfMonth();

        // 1) 최근 7일 표시용
        List<Boolean> weekDays = new ArrayList<>(7);
        for (int i = 6; i >= 0; i--) {
            weekDays.add(postedOn(memberId, today.minusDays(i)));
        }


        // 2) 이번 달 카운트(‘해당 날짜에 1개 이상 올렸는지’ 합계)
        int monthCount = 0;
        boolean monthPerfectSoFar = true;
        for (int d = 1; d <= today.getDayOfMonth(); d++) {
            boolean ok = postedOn(memberId, ym.atDay(d));
            if (ok) monthCount++;
            if (!ok) monthPerfectSoFar = false;
        }

        // 3) 보상 플래그
        int streak = currentStreak(memberId);
        boolean weekReward  = streak >= 7;

        // 해당 달을 하루도 빠지지 않고 채우면 보상(말일에만 true)
        boolean monthReward = (today.getDayOfMonth() == daysInMonth) && monthPerfectSoFar;

        return ChallengeStatusResponse.of(
                weekDays, 7,
                monthCount, daysInMonth,
                weekReward, monthReward
        );
    }
}
