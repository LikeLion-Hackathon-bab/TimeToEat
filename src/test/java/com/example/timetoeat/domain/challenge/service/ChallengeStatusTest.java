package com.example.timetoeat.domain.challenge.service;

import com.example.timetoeat.domain.article.repository.ArticleRepository;
import com.example.timetoeat.domain.challenge.dto.response.ChallengeStatusResponse;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** F74 밥 인증 챌린지 진행: 최근 7일 표시, 이번 달 일수, 7일 연속·월 개근 보상 플래그. KST 고정 시계. */
class ChallengeStatusTest {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    /** postedDays 날짜에 게시글이 있다고 답하는 저장소 대역으로 서비스를 만든다. 질의 구간도 함께 검사한다. */
    private static ChallengeQueryService service(LocalDate today, Set<LocalDate> postedDays) {
        var repo = mock(ArticleRepository.class);
        when(repo.countByAuthor_IdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(eq(1L), any(), any())).thenAnswer(inv -> {
            LocalDateTime start = inv.getArgument(1), end = inv.getArgument(2);
            assertEquals(LocalTime.MIDNIGHT, start.toLocalTime(), "하루 구간은 KST 자정부터");
            assertEquals(start.plusDays(1), end, "[자정, 다음 자정) 반열린 구간");
            return postedDays.contains(start.toLocalDate()) ? 1L : 0L;
        });
        Clock clock = Clock.fixed(today.atTime(9, 0).atZone(KST).toInstant(), KST);
        return new ChallengeQueryService(clock, repo);
    }

    private static Set<LocalDate> streak(LocalDate last, int days) {
        Set<LocalDate> s = new HashSet<>();
        for (int i = 0; i < days; i++) s.add(last.minusDays(i));
        return s;
    }

    @Test
    void nothingPosted() {
        var r = service(LocalDate.of(2026, 11, 28), Set.of()).getMyStatus(1L);
        assertEquals(List.of(false, false, false, false, false, false, false), r.getWeek().getDays());
        assertEquals(0, r.getWeek().getCompleted());
        assertEquals(7, r.getWeek().getGoal());
        assertEquals(0, r.getMonth().getCount());
        assertEquals(30, r.getMonth().getGoal());
        assertFalse(r.isWeekRewardAvailable());
        assertFalse(r.isMonthRewardAvailable());
    }

    @Test
    void weekDaysAreOldestFirstEndingToday() {
        LocalDate today = LocalDate.of(2026, 11, 28);
        var r = service(today, Set.of(today, today.minusDays(6))).getMyStatus(1L);
        assertEquals(List.of(true, false, false, false, false, false, true), r.getWeek().getDays());
        assertEquals(2, r.getWeek().getCompleted());
    }

    @Test
    void sixDayStreakIsNotEnoughSevenIs() {
        LocalDate today = LocalDate.of(2026, 11, 28);
        assertFalse(service(today, streak(today, 6)).getMyStatus(1L).isWeekRewardAvailable());
        assertTrue(service(today, streak(today, 7)).getMyStatus(1L).isWeekRewardAvailable());
    }

    @Test
    void streakMustIncludeTodaySoYesterdayEndingStreakGivesNoReward() {
        LocalDate today = LocalDate.of(2026, 11, 28);
        var r = service(today, streak(today.minusDays(1), 10)).getMyStatus(1L);
        assertEquals(6, r.getWeek().getCompleted(), "최근 7일 중 오늘만 빠짐");
        assertFalse(r.isWeekRewardAvailable(), "현재 구현: 오늘 인증 전에는 연속 기록 0");
    }

    @Test
    void streakCrossesMonthBoundary() {
        LocalDate today = LocalDate.of(2026, 12, 3);
        var r = service(today, streak(today, 7)).getMyStatus(1L);
        assertTrue(r.isWeekRewardAvailable(), "11/27~12/3 연속 7일");
        assertEquals(3, r.getMonth().getCount(), "이번 달 일수는 12월 것만");
        assertEquals(31, r.getMonth().getGoal());
    }

    @Test
    void monthRewardOnlyOnLastDayWithEveryDayPosted() {
        LocalDate last = LocalDate.of(2026, 11, 30);
        var perfect = service(last, streak(last, 30)).getMyStatus(1L);
        assertEquals(30, perfect.getMonth().getCount());
        assertTrue(perfect.isMonthRewardAvailable());

        Set<LocalDate> missing = streak(last, 30); missing.remove(LocalDate.of(2026, 11, 15));
        assertFalse(service(last, missing).getMyStatus(1L).isMonthRewardAvailable());

        LocalDate before = LocalDate.of(2026, 11, 29);
        assertFalse(service(before, streak(before, 29)).getMyStatus(1L).isMonthRewardAvailable(), "말일 전에는 false");
    }

    @Test
    void februaryOfLeapYear() {
        LocalDate last = LocalDate.of(2028, 2, 29);
        var r = service(last, streak(last, 29)).getMyStatus(1L);
        assertEquals(29, r.getMonth().getGoal());
        assertTrue(r.isMonthRewardAvailable());
    }

    @Test
    void streakLoopStopsAtSafetyLimit() {
        var repo = mock(ArticleRepository.class);
        when(repo.countByAuthor_IdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(anyLong(), any(), any())).thenReturn(1L);
        Clock clock = Clock.fixed(Instant.parse("2026-11-28T00:00:00Z"), KST);
        ChallengeStatusResponse r = new ChallengeQueryService(clock, repo).getMyStatus(1L);
        assertTrue(r.isWeekRewardAvailable());
        // 7(주) + 28(월) + 401(연속, 안전장치) 회. 성능 참고용 상한.
        verify(repo, times(7 + 28 + 401)).countByAuthor_IdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(anyLong(), any(), any());
    }
}
