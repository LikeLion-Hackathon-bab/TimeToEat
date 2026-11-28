package com.example.timetoeat.infra.ai;

import com.example.timetoeat.domain.recommendation.dto.MenuRecommendationResponse;

public interface RecommendGateway {
    /** AI 추천 서버 POST {reco-base-url}/v1/recommend 를 한 명(user_ids=[memberId])으로 호출한다. */
    MenuRecommendationResponse recommendForMember(Long memberId, int excludeDays, int topK);
}
