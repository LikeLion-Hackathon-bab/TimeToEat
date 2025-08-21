package com.example.timetoeat.domain.article.dto.ai;

import jakarta.validation.constraints.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InferenceResultDto {

    @NotBlank
    private String code;       // ex) "11015001"

    @NotBlank
    private String label;      // ex) "잡채"

    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private double confidence;

    @NotNull
    private OffsetDateTime timestamp; // ex) "2025-08-19T14:33:00+09:00"

    @Builder
    private InferenceResultDto(String code, String label, double confidence, OffsetDateTime timestamp) {
        this.code = code;
        this.label = label;
        this.confidence = confidence;
        this.timestamp = timestamp;
    }

    public static InferenceResultDto of(String code, String label, double confidence, OffsetDateTime ts) {
        return InferenceResultDto.builder()
                .code(code).label(label).confidence(confidence).timestamp(ts)
                .build();
    }
}
