package com.example.timetoeat.domain.article.dto.request;

import jakarta.validation.constraints.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiInferenceUpsertRequest {

    @NotBlank
    @Size(max = 16)
    private String code;

    @NotBlank
    @Size(max = 100)
    private String label;

    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private double confidence;

    @NotNull
    private OffsetDateTime timestamp;

    @Builder
    private AiInferenceUpsertRequest(String code, String label, double confidence, OffsetDateTime timestamp) {
        this.code = code;
        this.label = label;
        this.confidence = confidence;
        this.timestamp = timestamp;
    }
}
