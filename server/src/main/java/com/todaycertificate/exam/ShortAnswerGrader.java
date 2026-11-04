package com.todaycertificate.exam;

import java.util.Arrays;
import java.util.Locale;

/** 주관식 채점. accepted는 '|'로 구분한 허용 답 목록이고, 입력과 허용 답을 같은 규칙으로 정규화해서 비교한다. */
public final class ShortAnswerGrader {

    private ShortAnswerGrader() {
    }

    public static boolean isCorrect(String accepted, boolean exactMatch, String input) {
        if (input == null || input.isBlank()) {
            return false;
        }
        String answer = normalize(input, exactMatch);
        return Arrays.stream(accepted.split("\\|"))
                .anyMatch(a -> normalize(a, exactMatch).equals(answer));
    }

    /** exactMatch면 앞뒤 공백만 없앤다. 아니면 연속 공백도 하나로 줄이고 대소문자를 무시한다. */
    private static String normalize(String s, boolean exactMatch) {
        String trimmed = s.strip();
        return exactMatch ? trimmed : trimmed.replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
