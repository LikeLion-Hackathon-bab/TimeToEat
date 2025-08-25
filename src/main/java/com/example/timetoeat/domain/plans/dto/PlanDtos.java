package com.example.timetoeat.domain.plans.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

public class PlanDtos {
    // 작성자 서브 객체
    public record Author(
            @Schema(example = "string") @NotBlank String name
    ) {}

    // 생성 요청
    public record CreateRequest(
            @Schema(example = "2025-08-25")
            @NotNull
            @JsonFormat(pattern = "yyyy-MM-dd")
            LocalDate meetingDate,

            @Schema(example = "10:24")
            @NotNull
            @JsonFormat(pattern = "HH:mm")
            LocalTime meetingTime,

            @Schema(example = "서울특별시 노원구 공릉로 232 1층")
            @NotBlank
            String location,

            @NotNull Author author
    ) {}

    public record Response(
            Long id,
            @JsonFormat(pattern = "yyyy-MM-dd") LocalDate meetingDate,
            @JsonFormat(pattern = "HH:mm")       LocalTime meetingTime,
            String location,
            String authorName
    ) {}
}
