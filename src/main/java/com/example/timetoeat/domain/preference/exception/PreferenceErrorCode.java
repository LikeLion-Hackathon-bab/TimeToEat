package com.example.timetoeat.domain.preference.exception;

import com.example.timetoeat.global.error.BaseError;
import org.springframework.http.HttpStatus;

public enum PreferenceErrorCode implements BaseError {

    DUPLICATED_BETWEEN_LIKE_AND_DISLIKE(HttpStatus.BAD_REQUEST, "같은 음식(코드)을 선호와 비선호로 동시에 선택할 수 없습니다."),
    CODE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 음식 코드입니다."),
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),
    ;

    private final HttpStatus httpStatus;
    private final String message;

    PreferenceErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }

    @Override
    public String getCode() {
        return name();
    }

    @Override
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
