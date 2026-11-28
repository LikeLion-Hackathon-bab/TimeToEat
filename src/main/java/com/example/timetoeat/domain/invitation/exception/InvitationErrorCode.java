package com.example.timetoeat.domain.invitation.exception;

import com.example.timetoeat.global.error.BaseError;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum InvitationErrorCode implements BaseError {

    ALREADY_PENDING(HttpStatus.CONFLICT, "INVITATION_ALREADY_PENDING", "이미 보낸 초대장이 있어요.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    InvitationErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus; this.code = code; this.message = message;
    }
}
