package com.todaycertificate.exam.upload;

/**
 * row: 헤더를 뺀 1부터의 문제 순번 (파일 전체 오류는 0).
 * 여러 줄짜리 칸이 있어서 텍스트 편집기의 줄 번호와는 다를 수 있다.
 */
public record CsvError(int row, String message) {
}
