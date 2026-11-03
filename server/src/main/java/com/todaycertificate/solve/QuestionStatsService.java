package com.todaycertificate.solve;

import com.todaycertificate.exam.Question;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 문제별 정답률. 전체 회원의 모든 풀이를 센다. 5번 미만으로 풀린 문제는 표본이 적어서 null.
 * 정답 판정은 기록 당시가 아니라 현재 정답번호 기준이다 (운영자가 정답을 고쳐 다시 올린 경우에도 맞게).
 */
@Component
public class QuestionStatsService {

    static final int MIN_SOLVED = 5;

    private final SolveRecordRepository records;

    public QuestionStatsService(SolveRecordRepository records) {
        this.records = records;
    }

    /** mostChosenWrongNo: 가장 많이 고른 오답 보기. 아무도 틀리지 않았으면 null. */
    public record Stats(long solvedCount, int correctRate, Integer mostChosenWrongNo) {
    }

    /** 문제 ID → 통계. 표본이 적은 문제는 맵에 없다 (get 하면 null). 쿼리 한 번. */
    public Map<Long, Stats> statsFor(List<Question> questions) {
        Map<Long, long[]> counts = new HashMap<>(); // 문제 ID → 보기 1~4번을 고른 횟수 (인덱스 0은 안 씀)
        if (!questions.isEmpty()) {
            for (SolveRecordRepository.ChoiceCount c : records.countChoices(
                    questions.stream().map(Question::getId).toList())) {
                counts.computeIfAbsent(c.getQuestionId(), k -> new long[5])[c.getSelectedNo()] = c.getCount();
            }
        }

        Map<Long, Stats> result = new HashMap<>();
        for (Question q : questions) {
            long[] byChoice = counts.getOrDefault(q.getId(), new long[5]);
            long total = byChoice[1] + byChoice[2] + byChoice[3] + byChoice[4];
            if (total < MIN_SOLVED) {
                continue;
            }
            Integer mostWrong = null;
            for (int no = 1; no <= 4; no++) {
                if (no != q.getAnswerNo() && byChoice[no] > 0 && (mostWrong == null || byChoice[no] > byChoice[mostWrong])) {
                    mostWrong = no;
                }
            }
            result.put(q.getId(), new Stats(total, (int) (byChoice[q.getAnswerNo()] * 100 / total), mostWrong));
        }
        return result;
    }
}
