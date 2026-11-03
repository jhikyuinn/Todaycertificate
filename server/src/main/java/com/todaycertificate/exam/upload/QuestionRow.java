package com.todaycertificate.exam.upload;

import java.util.List;

/** CSV 한 줄 = 문제 하나. choices는 항상 4개, explanation은 비어 있으면 null. */
public record QuestionRow(int subjectNumber, int number, String content, List<String> choices,
                          int answerNo, String explanation) {
}
