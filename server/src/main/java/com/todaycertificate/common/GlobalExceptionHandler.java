package com.todaycertificate.common;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** 모든 컨트롤러에서 올라온 예외를 한곳에서 받아 ErrorResponse로 바꾼다. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 안내 문구의 한도를 설정값에서 읽어서, 한도를 바꿔도 문구가 따로 놀지 않게 한다
    @Value("${spring.servlet.multipart.max-file-size}")
    private DataSize maxFileSize;

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

    // 필수 파라미터·파일 누락, 숫자 자리에 문자 등 요청 형식 오류
    @ExceptionHandler({MissingServletRequestParameterException.class, MissingServletRequestPartException.class,
            MethodArgumentTypeMismatchException.class})
    ResponseEntity<ErrorResponse> handleBadRequest(Exception e) {
        return ErrorResponse.of(ErrorCode.VALIDATION_FAILED, List.of());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ErrorResponse> handleTooLarge(MaxUploadSizeExceededException e) {
        return ErrorResponse.of(ErrorCode.VALIDATION_FAILED,
                List.of(new FieldError("file", "파일이 너무 큽니다 (최대 " + maxFileSize.toMegabytes() + "MB).")));
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
