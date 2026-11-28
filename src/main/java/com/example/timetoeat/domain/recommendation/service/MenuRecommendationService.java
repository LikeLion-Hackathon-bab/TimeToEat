package com.example.timetoeat.domain.recommendation.service;

import com.example.timetoeat.domain.recommendation.dto.MenuRecommendationResponse;
import com.example.timetoeat.infra.ai.RecommendGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class MenuRecommendationService {

    /** 기획 문서 §8.5: 최근 72시간(3일) 식사 제외 */
    private static final int EXCLUDE_DAYS = 3;

    private final RecommendGateway gateway;

    public MenuRecommendationResponse recommendForMe(Long meId, int topK) {
        int k = Math.max(1, Math.min(topK, 10));
        try {
            return gateway.recommendForMember(meId, EXCLUDE_DAYS, k);
        } catch (RuntimeException e) {
            log.warn("AI recommend failed member={} msg={}", meId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "메뉴 추천 서버에 연결하지 못했어요.");
        }
    }
}
