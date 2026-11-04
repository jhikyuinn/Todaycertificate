package com.todaycertificate.exam.pdf;

import com.todaycertificate.exam.QuestionType;
import java.util.ArrayList;
import java.util.List;

/**
 * PDF에서 읽은 문제 하나. 저장 전 미리보기용이라 형식이 틀려도 버리지 않고 warnings에 이유를 담는다.
 * subjectNumber는 못 찾으면 0, answerNo·answerText·explanation은 못 찾으면 null.
 * 객관식은 choices·answerNo, 주관식(실기)은 answerText만 채운다 (choices는 빈 목록).
 */
public record PreviewRow(int subjectNumber, int number, QuestionType type, String content, List<String> choices,
                         Integer answerNo, String answerText, String explanation, List<String> warnings) {

    PreviewRow withWarning(String warning) {
        List<String> all = new ArrayList<>(warnings);
        all.add(warning);
        return new PreviewRow(subjectNumber, number, type, content, choices, answerNo, answerText, explanation, all);
    }
}
