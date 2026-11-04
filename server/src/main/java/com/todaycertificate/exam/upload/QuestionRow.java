package com.todaycertificate.exam.upload;

import com.todaycertificate.exam.QuestionType;
import java.util.List;

/**
 * CSV 한 줄 = 문제 하나. explanation은 비어 있으면 null.
 * 객관식: choices 4개, answerNo 1~4, answerText null. 주관식: choices 비어 있음, answerNo null.
 */
public record QuestionRow(int subjectNumber, int number, String content, List<String> choices,
                          Integer answerNo, String explanation,
                          QuestionType type, String answerText, boolean exactMatch) {
}
