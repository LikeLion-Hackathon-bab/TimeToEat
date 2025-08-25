package com.example.timetoeat.domain.meal.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UpdateMealStatusRequest {

    public enum Action { ATE_NOW, SET_OFF } // ON(밥 먹음) / OFF(공복 시작)

    @NotNull
    private Action action;
}