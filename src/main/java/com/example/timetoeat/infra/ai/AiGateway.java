package com.example.timetoeat.infra.ai;

import java.time.OffsetDateTime;

public interface AiGateway {

    void requestInference(Long articleId, Long userId, String imageUrl,
                          OffsetDateTime mealAtKst, String callbackUrl);
}
