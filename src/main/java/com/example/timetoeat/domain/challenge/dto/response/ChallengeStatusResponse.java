package com.example.timetoeat.domain.challenge.dto.response;

import lombok.*;

import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChallengeStatusResponse {

    private WeekProgress week;
    private MonthProgress month;

    private boolean weekRewardAvailable; // 주간 목표 달성 시 true
    private boolean monthRewardAvailable;  // 월간 목표 달성 시 true

    @Builder
    private ChallengeStatusResponse(WeekProgress week, MonthProgress month,
                                    boolean weekRewardAvailable, boolean monthRewardAvailable) {
        this.week = week;
        this.month = month;
        this.weekRewardAvailable = weekRewardAvailable;
        this.monthRewardAvailable = monthRewardAvailable;
    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class WeekProgress {
        private List<Boolean> days;
        private int completed;
        private int goal;

        @Builder
        private WeekProgress(List<Boolean> days, int completed, int goal) {
            this.days = days;
            this.completed = completed;
            this.goal = goal;
        }
    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class MonthProgress {
        private int count; // 이번 달 ‘게시한 일수’
        private int goal;  // 이번 달 '전체 일수'

        @Builder
        private MonthProgress(int count, int goal) {
            this.count = count;
            this.goal = goal;
        }
    }

    public static ChallengeStatusResponse of(List<Boolean> weekDays, int weekGoal,
                                             int monthCount, int monthGoal,
                                             boolean weekReward, boolean monthReward) {
        int done = (int) weekDays.stream().filter(Boolean::booleanValue).count();
        return ChallengeStatusResponse.builder()
                .week(WeekProgress.builder().days(weekDays).completed(done).goal(weekGoal).build())
                .month(MonthProgress.builder().count(monthCount).goal(monthGoal).build())
                .weekRewardAvailable(weekReward)
                .monthRewardAvailable(monthReward)
                .build();
    }
}
