package com.todaycertificate.attempt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 국가기술자격 필기 채점 규칙. DB와 무관한 순수 계산이라 단위 테스트로 규칙을 고정한다.
 * - 과목 점수 = 맞힌 수 × 100 / 문항 수 (소수점 버림)
 * - 과목 점수 < failScore 이면 과락
 * - 평균(소수 첫째 자리 반올림) ≥ passAverage 이고 과락이 없으면 합격
 */
public final class ScoreCalculator {

    private ScoreCalculator() {
    }

    public record GradedQuestion(int subjectNumber, String subjectName, boolean correct) {
    }

    public record SubjectScore(int number, String name, int score, boolean failed) {
    }

    public record ExamScore(List<SubjectScore> subjects, double averageScore, boolean passed) {
    }

    public static ExamScore score(List<GradedQuestion> graded, int failScore, int passAverage) {
        // 과목번호 순서를 유지하려고 TreeMap. 값 = {맞힌 수, 문항 수}
        Map<Integer, int[]> counts = new TreeMap<>();
        Map<Integer, String> names = new TreeMap<>();
        for (GradedQuestion g : graded) {
            int[] c = counts.computeIfAbsent(g.subjectNumber(), k -> new int[2]);
            if (g.correct()) {
                c[0]++;
            }
            c[1]++;
            names.putIfAbsent(g.subjectNumber(), g.subjectName());
        }

        List<SubjectScore> subjects = new ArrayList<>();
        int sum = 0;
        for (Map.Entry<Integer, int[]> e : counts.entrySet()) {
            int score = e.getValue()[0] * 100 / e.getValue()[1];
            subjects.add(new SubjectScore(e.getKey(), names.get(e.getKey()), score, score < failScore));
            sum += score;
        }
        double average = subjects.isEmpty() ? 0 : Math.round(sum * 10.0 / subjects.size()) / 10.0;
        boolean passed = !subjects.isEmpty() && average >= passAverage
                && subjects.stream().noneMatch(SubjectScore::failed);
        return new ExamScore(subjects, average, passed);
    }
}
