package com.todaycertificate.exam.upload;

import java.util.List;

/** errors가 하나라도 있으면 rows는 저장하지 않는다 (업로드 전체 거부). */
public record CsvParseResult(List<QuestionRow> rows, List<CsvError> errors) {

    static CsvParseResult fileError(String message) {
        return new CsvParseResult(List.of(), List.of(new CsvError(0, message)));
    }
}
