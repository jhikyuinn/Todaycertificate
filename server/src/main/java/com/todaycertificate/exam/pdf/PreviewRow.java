package com.todaycertificate.exam.pdf;

import java.util.List;

/**
 * PDF에서 읽은 문제 하나. 저장 전 미리보기용이라 형식이 틀려도 버리지 않고 warnings에 이유를 담는다.
 * subjectNumber는 못 찾으면 0, answerNo·explanation은 못 찾으면 null.
 */
public record PreviewRow(int subjectNumber, int number, String content, List<String> choices,
                         Integer answerNo, String explanation, List<String> warnings) {
}
