package com.example.timetoeat.domain.meal.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MealStatusResponse {

    public enum Status { FED, FASTING }

    private Status status;  // ON(FED) / OFF(FASTING)
    private LocalDateTime lastMealAt;
    private long fastingMinutes;  // FASTING(공복상태)일 때 공복 분
    private long fastingHours;  // FASTING일 때 공복 시간(내림)
    private long secondsToAutoOff;  // FED(음식 섭취)일 때 자동 OFF까지 남은 초(<= 4h)

    @Builder
    private MealStatusResponse(Status status, LocalDateTime lastMealAt,
                               long fastingMinutes, long fastingHours, long secondsToAutoOff) {
        this.status = status;
        this.lastMealAt = lastMealAt;
        this.fastingMinutes = fastingMinutes;
        this.fastingHours = fastingHours;
        this.secondsToAutoOff = secondsToAutoOff;
    }

    public static MealStatusResponse fed(LocalDateTime lastMealAt, long secondsToAutoOff) {
        return MealStatusResponse.builder()
                .status(Status.FED).lastMealAt(lastMealAt)
                .fastingMinutes(0).fastingHours(0)
                .secondsToAutoOff(secondsToAutoOff)
                .build();
    }

    public static MealStatusResponse fasting(LocalDateTime lastMealAt, long fastingMin, long fastingHr) {
        return MealStatusResponse.builder()
                .status(Status.FASTING).lastMealAt(lastMealAt)
                .fastingMinutes(fastingMin).fastingHours(fastingHr)
                .secondsToAutoOff(0)
                .build();
    }
}
