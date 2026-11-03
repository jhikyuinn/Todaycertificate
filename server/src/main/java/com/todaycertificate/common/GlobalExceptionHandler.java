package com.todaycertificate.common;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** 모든 컨트롤러에서 올라온 예외를 한곳에서 받아 ErrorResponse로 바꾼다. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    record FieldError(String field, String message) {
    }

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ErrorResponse> handleApi(ApiException e) {
        return ErrorResponse.of(e.getCode(), e.getDetails());
    }

    // @Valid 검증 실패: 어떤 칸이 왜 틀렸는지 details에 담는다
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        List<FieldError> details = e.getBindingResult().getFieldErrors().stream()
                .map(f -> new FieldError(f.getField(), f.getDefaultMessage()))
                .toList();
        return ErrorResponse.of(ErrorCode.VALIDATION_FAILED, details);
    }

    // JSON 문법 오류, 타입이 맞지 않는 값
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException e) {
        return ErrorResponse.of(ErrorCode.VALIDATION_FAILED, List.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException e) {
        return ErrorResponse.of(ErrorCode.NOT_FOUND, List.of());
    }

    // 예상하지 못한 오류: 내부 메시지는 로그에만 남기고 응답에는 노출하지 않는다
    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        log.error("Unexpected error", e);
        return ErrorResponse.of(ErrorCode.INTERNAL_ERROR, List.of());
    }
}
