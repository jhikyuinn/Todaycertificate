package com.todaycertificate.solve;

import com.todaycertificate.exam.Question;
import com.todaycertificate.exam.QuestionType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * 문제별 정답률. 전체 회원의 모든 풀이를 센다. 5번 미만으로 풀린 문제는 표본이 적어서 null.
 * 정답 판정은 기록 당시가 아니라 현재 정답 기준이다 (운영자가 정답을 고쳐 다시 올린 경우에도 맞게).
 */
@Component
public class QuestionStatsService {

    static final int MIN_SOLVED = 5;

    private final SolveRecordRepository records;

    public QuestionStatsService(SolveRecordRepository records) {
        this.records = records;
    }

    /** mostChosenWrongNo: 가장 많이 고른 오답 보기. 아무도 틀리지 않았거나 주관식이면 null. */
    public record Stats(long solvedCount, int correctRate, Integer mostChosenWrongNo) {
    }

    /** 문제 ID → 통계. 표본이 적은 문제는 맵에 없다 (get 하면 null). 쿼리 한 번. */
    public Map<Long, Stats> statsFor(List<Question> questions) {
        Map<Long, Question> byId = questions.stream().collect(Collectors.toMap(Question::getId, Function.identity()));
        // 문제 ID → [0] 정답 횟수, [1~4] 그 보기를 고른 오답 횟수, [5] 전체 횟수
        Map<Long, long[]> counts = new HashMap<>();
        if (!byId.isEmpty()) {
            for (SolveRecordRepository.ChoiceCount c : records.countChoices(byId.keySet())) {
                long[] n = counts.computeIfAbsent(c.getQuestionId(), k -> new long[6]);
                n[5] += c.getCount();
                if (byId.get(c.getQuestionId()).isCorrect(c.getSelectedNo(), c.getAnswerText())) {
                    n[0] += c.getCount();
                } else if (c.getSelectedNo() != null) {
                    n[c.getSelectedNo()] += c.getCount();
                }
            }
        }

        Map<Long, Stats> result = new HashMap<>();
        for (Question q : questions) {
            long[] n = counts.getOrDefault(q.getId(), new long[6]);
            if (n[5] < MIN_SOLVED) {
                continue;
            }
            Integer mostWrong = null;
            // 다시 업로드로 주관식이 된 문제에 예전 객관식 기록이 남아 있어도 보기 번호는 내보내지 않는다
            if (q.getType() == QuestionType.MULTIPLE_CHOICE) {
                for (int no = 1; no <= 4; no++) {
                    if (n[no] > 0 && (mostWrong == null || n[no] > n[mostWrong])) {
                        mostWrong = no;
                    }
                }
            }
            result.put(q.getId(), new Stats(n[5], (int) (n[0] * 100 / n[5]), mostWrong));
        }
        return result;
    }
}
