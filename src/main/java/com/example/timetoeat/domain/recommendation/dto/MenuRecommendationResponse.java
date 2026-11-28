package com.example.timetoeat.domain.recommendation.dto;

import java.util.List;

/** 개인화 메뉴 추천(SCR-24) 응답. excludedCodes = AI 서버가 최근 식사·알레르기 등으로 제외한 음식 코드. */
public record MenuRecommendationResponse(
        List<Item> items,
        List<String> excludedCodes
) {
    public record Item(String code, String label, Double score, List<String> reasons) {
    }
}
