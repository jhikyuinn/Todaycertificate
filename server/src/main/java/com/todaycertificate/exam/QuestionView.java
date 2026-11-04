package com.todaycertificate.exam;

import java.util.List;

/** 풀기 전 화면에 보내는 문제. 정답과 해설은 절대 넣지 않는다. 주관식은 choices가 빈 목록. */
public record QuestionView(Long id, int number, QuestionType type, int subjectNumber, String subjectName,
                           String content, List<String> choices, boolean bookmarked) {

    static QuestionView of(Question q, boolean bookmarked) {
        return new QuestionView(q.getId(), q.getNumber(), q.getType(), q.getSubject().getNumber(),
                q.getSubject().getName(), q.getContent(), q.getChoices(), bookmarked);
    }
}
