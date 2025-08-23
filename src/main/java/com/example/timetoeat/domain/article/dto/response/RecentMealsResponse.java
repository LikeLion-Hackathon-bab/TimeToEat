package com.example.timetoeat.domain.article.dto.response;

import com.example.timetoeat.domain.article.entity.MealLog;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecentMealsResponse {

    private Long memberId;
    private int rangeHours; // 3일(72시간)
    private List<Item> items; // code/label 별 집계(중복 제거)

    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class Item {

        private String code;
        private String label;
        private long count;  // 해당 기간에 몇 번 먹었는지
        private LocalDateTime lastAteAt;  // KST, 마지막 먹은 시각

        private Item(String code, String label, long count, LocalDateTime lastAteAt) {
            this.code = code;
            this.label = label;
            this.count = count;
            this.lastAteAt = lastAteAt;
        }
    }

    private RecentMealsResponse(Long memberId, int rangeHours, List<Item> items) {
        this.memberId = memberId;
        this.rangeHours = rangeHours;
        this.items = items;
    }

    public static RecentMealsResponse from(Long memberId, List<MealLog> logs) {

        ZoneId KST = ZoneId.of("Asia/Seoul");

        // code 기준 집계: count, lastAt, label(가장 최근 label 채택)
        Map<String, List<MealLog>> byCode = logs.stream()
                .collect(Collectors.groupingBy(MealLog::getCode, LinkedHashMap::new, Collectors.toList()));

        List<Item> items = byCode.entrySet().stream()
                .map(e -> {
                    String code = e.getKey();
                    List<MealLog> group = e.getValue();
                    long count = group.size();
                    MealLog latest = Collections.max(group, Comparator.comparing(MealLog::getTsUtc));
                    LocalDateTime lastAt = LocalDateTime.ofInstant(latest.getTsUtc(), KST);
                    String label = latest.getLabel(); // 최신 라벨
                    return new Item(code, label, count, lastAt);
                })
                // 최근에 먹은 순으로 정렬
                .sorted(Comparator.comparing(Item::getLastAteAt).reversed())
                .toList();

        return new RecentMealsResponse(memberId, 72, items);
    }
}
