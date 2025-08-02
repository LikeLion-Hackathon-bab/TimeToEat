package com.example.timetoeat.global.error;

import org.springframework.http.HttpStatus;

public interface BaseError {
    HttpStatus getHttpStatus();
    String getCode();
    String getMessage();
}
