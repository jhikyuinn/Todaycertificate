package com.todaycertificate.attempt;

import static org.assertj.core.api.Assertions.assertThat;

import com.todaycertificate.attempt.ScoreCalculator.ExamScore;
import com.todaycertificate.attempt.ScoreCalculator.GradedQuestion;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ScoreCalculatorTest {

    @Test
    void subjectScore_15of20_is75() {
        assertThat(score(subject(1, 15, 20)).subjects().get(0).score()).isEqualTo(75);
    }

    @Test
    void subjectScore_8of20_is40_notFailed() {
        var s = score(subject(1, 8, 20)).subjects().get(0);
        assertThat(s.score()).isEqualTo(40);
        assertThat(s.failed()).isFalse();
    }

    @Test
    void subjectScore_7of20_is35_failed() {
        var s = score(subject(1, 7, 20)).subjects().get(0);
        assertThat(s.score()).isEqualTo(35);
        assertThat(s.failed()).isTrue();
    }

    @Test
    void highAverageWithOneFailedSubject_notPassed() {
        ExamScore r = score(subject(1, 20, 20), subject(2, 20, 20), subject(3, 7, 20));
        assertThat(r.averageScore()).isEqualTo(78.3);
        assertThat(r.passed()).isFalse();
    }

    @Test
    void average60_noFail_passed() {
        ExamScore r = score(subject(1, 12, 20), subject(2, 12, 20));
        assertThat(r.averageScore()).isEqualTo(60.0);
        assertThat(r.passed()).isTrue();
    }

    @Test
    void average_isRoundedToOneDecimal() {
        assertThat(score(subject(1, 15, 20), subject(2, 7, 20), subject(3, 8, 20)).averageScore()).isEqualTo(50.0);
    }

    @Test
    void subjects_areOrderedByNumber() {
        ExamScore r = score(subject(2, 1, 1), subject(1, 1, 1));
        assertThat(r.subjects()).extracting(ScoreCalculator.SubjectScore::number).containsExactly(1, 2);
    }

    @SafeVarargs
    static ExamScore score(List<GradedQuestion>... subjects) {
        List<GradedQuestion> all = new ArrayList<>();
        for (List<GradedQuestion> s : subjects) {
            all.addAll(s);
        }
        return ScoreCalculator.score(all, 40, 60);
    }

    static List<GradedQuestion> subject(int number, int correct, int total) {
        List<GradedQuestion> list = new ArrayList<>();
        for (int i = 0; i < total; i++) {
            list.add(new GradedQuestion(number, number + "과목", i < correct));
        }
        return list;
    }
}
