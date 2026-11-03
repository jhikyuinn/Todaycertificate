package com.todaycertificate.common;

import java.util.List;
import org.springframework.http.ResponseEntity;

/** 모든 오류 응답의 모양: {"code": "...", "message": "...", "details": [...]} */
public record ErrorResponse(String code, String message, List<?> details) {

    public static ResponseEntity<ErrorResponse> of(ErrorCode code, List<?> details) {
        return ResponseEntity.status(code.status())
                .body(new ErrorResponse(code.name(), code.message(), details));
    }
}
