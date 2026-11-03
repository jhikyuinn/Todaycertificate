package com.todaycertificate.common;

import java.util.List;

/** 서비스 코드에서 던지면 GlobalExceptionHandler가 ErrorResponse로 바꿔 응답한다. */
public class ApiException extends RuntimeException {

    private final ErrorCode code;
    private final List<?> details;

    public ApiException(ErrorCode code) {
        this(code, List.of());
    }

    public ApiException(ErrorCode code, List<?> details) {
        super(code.message());
        this.code = code;
        this.details = details;
    }

    public ErrorCode getCode() { return code; }
    public List<?> getDetails() { return details; }
}
