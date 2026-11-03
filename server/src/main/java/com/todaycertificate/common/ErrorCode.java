package com.todaycertificate.common;

import org.springframework.http.HttpStatus;

/** 서버가 내는 모든 오류의 종류. 화면은 code 값으로 분기한다. */
public enum ErrorCode {

    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
    CSV_INVALID(HttpStatus.BAD_REQUEST, "CSV 파일에 오류가 있습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "권한이 없습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 데이터를 찾을 수 없습니다."),
    DUPLICATE_LOGIN_ID(HttpStatus.CONFLICT, "이미 사용 중인 아이디입니다."),
    DUPLICATE_NAME(HttpStatus.CONFLICT, "이미 있는 이름입니다."),
    SUBJECT_IN_USE(HttpStatus.CONFLICT, "문제가 등록된 과목은 삭제할 수 없습니다."),
    ALREADY_SUBMITTED(HttpStatus.CONFLICT, "이미 제출한 시험입니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버에 문제가 발생했습니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus status() { return status; }
    public String message() { return message; }
}
