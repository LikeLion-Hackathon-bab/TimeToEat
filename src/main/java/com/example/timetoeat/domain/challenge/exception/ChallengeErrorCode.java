package com.example.timetoeat.domain.challenge.exception;

import com.example.timetoeat.global.error.BaseError;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ChallengeErrorCode implements BaseError {

    REWARD_NOT_AVAILABLE(HttpStatus.BAD_REQUEST, "REWARD_NOT_AVAILABLE", "보상(쿠폰)을 수령할 수 있는 상태가 아닙니다."),
    ALREADY_CLAIMED(HttpStatus.CONFLICT, "ALREADY_CLAIMED", "이미 해당 보상(쿠폰)을 수령했습니다."),
    INVALID_REWARD_TYPE(HttpStatus.BAD_REQUEST, "INVALID_REWARD_TYPE", "잘못된 보상(쿠폰) 타입입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    ChallengeErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}
