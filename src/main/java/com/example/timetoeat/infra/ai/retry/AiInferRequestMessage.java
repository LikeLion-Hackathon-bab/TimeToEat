package com.example.timetoeat.infra.ai.retry;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class AiInferRequestMessage {

    private Long articleId;
    private Long authorId;
    private String imagePath;
    private LocalDateTime mealAtKst;
}
