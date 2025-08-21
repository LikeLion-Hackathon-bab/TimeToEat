package com.example.timetoeat.infra.ai;

import com.example.timetoeat.domain.article.dto.ai.InferenceResultDto;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

public interface AiGateway {

    // 표준: 동기 호출 (form-data 파일 1장)
    InferenceResultDto inferFoodSync(Long articleId, Long authorId,
                                     String imagePath, LocalDateTime mealAtKst);

    // 콜백 전용(이제 사용 안 함)
    @Deprecated
    void requestInference(Long articleId, Long userId, String imageUrl,
                          OffsetDateTime mealAtKst, String callbackUrl);
}
