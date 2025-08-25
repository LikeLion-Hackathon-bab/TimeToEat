package com.example.timetoeat.domain.referral.exception;

import com.example.timetoeat.global.error.BaseError;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ReferralErrorCode implements BaseError {

    CODE_NOT_FOUND(HttpStatus.NOT_FOUND, "REFERRAL_CODE_NOT_FOUND", "유효하지 않은 추천 코드입니다."),
    CODE_EXPIRED(HttpStatus.BAD_REQUEST, "REFERRAL_CODE_EXPIRED", "만료된 추천 코드입니다."),
    CODE_ALREADY_USED(HttpStatus.CONFLICT, "REFERRAL_CODE_ALREADY_USED", "이미 사용된 추천 코드입니다."),
    SELF_REFERRAL_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "SELF_REFERRAL_NOT_ALLOWED", "자기 자신은 추천 등록할 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    ReferralErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus; this.code = code; this.message = message;
    }
}
