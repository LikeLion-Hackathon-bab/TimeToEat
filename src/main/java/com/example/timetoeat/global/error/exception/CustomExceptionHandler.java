package com.example.timetoeat.global.error.exception;

import com.example.timetoeat.global.common.ApiResponse;
import com.example.timetoeat.global.error.GlobalErrorCode;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
public class CustomExceptionHandler {
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse> handleBusinessException(CustomException e) {
        // 4xx 는 클라이언트 입력·인증 상태에 따른 정상 응답이라 스택 없이 WARN. 5xx 만 ERROR + 스택.
        if (e.getBaseError().getHttpStatus().is5xxServerError()) {
            log.error("customException : {}", e.getBaseError().getMessage(), e);
        } else {
            log.warn("customException : {} ({})", e.getBaseError().getCode(), e.getMessage());
        }
        return ResponseEntity
                .status(e.getBaseError().getHttpStatus())
                .body(ApiResponse.fail(e.getBaseError()));
    }

    @ExceptionHandler({
            MethodArgumentTypeMismatchException.class,
            IllegalArgumentException.class,
            HttpMessageNotReadableException.class,
            InvalidFormatException.class,
            ServletRequestBindingException.class
    })
    public ResponseEntity<ApiResponse> handleBadRequestException(Exception e) {
        log.error("Exception : {}", e.getMessage(), e);
        return ResponseEntity
                .status(GlobalErrorCode.BAD_REQUEST.getHttpStatus())
                .body(ApiResponse.fail(GlobalErrorCode.BAD_REQUEST));
    }

    @ExceptionHandler(ChangeSetPersister.NotFoundException.class)
    public ResponseEntity<ApiResponse> handleNotFoundException(Exception e) {
        log.error("NotFoundException : {}", e.getMessage(), e);
        return ResponseEntity
                .status(GlobalErrorCode.NOT_FOUND.getHttpStatus())
                .body(ApiResponse.fail(GlobalErrorCode.NOT_FOUND));
    }

    // --- [추가] JSON 직렬화 에러를 위한 전용 핸들러 ---
    @ExceptionHandler(HttpMessageNotWritableException.class)
    public ResponseEntity<ApiResponse<Object>> handleSerializationException(HttpMessageNotWritableException e) {
        // 이 로그가 찍힌다면, 100% JSON 변환 문제입니다.
        log.error("JSON 직렬화 에러 발생 (Response Body 생성 실패): {}", e.getMessage(), e);

        return ResponseEntity
                .status(GlobalErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus())
                .body(ApiResponse.fail(GlobalErrorCode.INTERNAL_SERVER_ERROR, "서버가 응답을 생성하는데 실패했습니다."));
    }

    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Object>> handleStatus(org.springframework.web.server.ResponseStatusException error) {
        var status = error.getStatusCode();
        String message = error.getReason() != null ? error.getReason() :
                org.springframework.http.HttpStatus.valueOf(status.value()).getReasonPhrase();
        return ResponseEntity.status(status).body(ApiResponse.builder()
                .code("HTTP_" + status.value()).message(message).build());
    }

    /** 없는 경로(정적 리소스 탐색 실패 포함)는 404. 이전에는 아래 Exception 처리로 500이었다. */
    @ExceptionHandler({org.springframework.web.servlet.resource.NoResourceFoundException.class,
            org.springframework.web.servlet.NoHandlerFoundException.class})
    public ResponseEntity<ApiResponse<Object>> handleNoRoute(Exception e) {
        return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND)
                .body(ApiResponse.builder().code("HTTP_404").message("요청한 경로를 찾을 수 없습니다.").build());
    }

    /** 컨트롤러가 직접 던진 권한 거부(예: X-AI-KEY 불일치)는 403(이전 500). */
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleDenied(org.springframework.security.access.AccessDeniedException e) {
        return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                .body(ApiResponse.builder().code("HTTP_403").message("접근 권한이 없습니다.").build());
    }

    /** 지원하지 않는 메서드는 405(이전 500). */
    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Object>> handleMethod(org.springframework.web.HttpRequestMethodNotSupportedException e) {
        return ResponseEntity.status(org.springframework.http.HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.builder().code("HTTP_405").message("지원하지 않는 요청 방식입니다.").build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse> handleException(Exception e) {
        log.error("Exception : {}", e.getMessage(), e);
        return ResponseEntity
                .status(GlobalErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus())
                .body(ApiResponse.fail(GlobalErrorCode.INTERNAL_SERVER_ERROR));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationException(MethodArgumentNotValidException ex) {
        // 모든 필드 오류 메시지를 하나의 문자열로 묶기
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .reduce((msg1, msg2) -> msg1 + ", " + msg2)
                .orElse(GlobalErrorCode.BAD_REQUEST.getMessage());

        log.warn("Validation failed: {}", message);

        return ResponseEntity
                .status(GlobalErrorCode.BAD_REQUEST.getHttpStatus())
                .body(ApiResponse.fail(GlobalErrorCode.BAD_REQUEST, message));
    }

    @ExceptionHandler(SpelEvaluationException.class)
    public ResponseEntity<ApiResponse> handleSpel(SpelEvaluationException ex) {
        log.warn("Invalid principal (SpEL): {}", ex.getMessage());
        return ResponseEntity
                .status(GlobalErrorCode.UNAUTHORIZED.getHttpStatus())
                .body(ApiResponse.fail(GlobalErrorCode.UNAUTHORIZED, "잘못된 사용자 정보입니다."));
    }
}
