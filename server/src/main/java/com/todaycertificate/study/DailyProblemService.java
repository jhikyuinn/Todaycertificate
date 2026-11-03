package com.todaycertificate.study;

import com.todaycertificate.exam.Question;
import com.todaycertificate.exam.QuestionRepository;
import com.todaycertificate.exam.QuestionView;
import com.todaycertificate.exam.QuestionViewAssembler;
import com.todaycertificate.solve.SolveRecordRepository;
import com.todaycertificate.solve.SolveRecordRepository.LatestSolve;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 오늘 풀 문제 최대 20개. 앞 묶음부터 채운다.
 * 1) 오늘 아직 안 푼 오답노트 문제 최대 8개 → 2) 과락 위험 과목의 안 푼 문제 최대 6개
 * → 3) 안 푼 문제 → 4) 이미 푼 문제.
 * 묶음 안에서는 (회원, 오늘 날짜)를 시드로 섞는다. 시드가 같으면 섞는 결과도 같아서, 기록이 그대로면 하루 동안 같은 목록이다.
 */
@Service
public class DailyProblemService {

    static final int LIMIT = 20;
    static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final StudyTarget target;
    private final SubjectReportService reports;
    private final QuestionRepository questions;
    private final SolveRecordRepository records;
    private final QuestionViewAssembler assembler;

    public DailyProblemService(StudyTarget target, SubjectReportService reports, QuestionRepository questions,
                               SolveRecordRepository records, QuestionViewAssembler assembler) {
        this.target = target;
        this.reports = reports;
        this.questions = questions;
        this.records = records;
        this.assembler = assembler;
    }

    @Transactional(readOnly = true)
    public List<QuestionView> dailyProblems(Long memberId) {
        Optional<Long> certificateId = target.certificateId(memberId);
        if (certificateId.isEmpty()) {
            return List.of();
        }
        // shortcut: 자격증의 전체 문제를 메모리에 올려 고른다. 문제가 수만 개가 되면 SQL에서 후보를 줄여야 한다
        List<Question> all = questions.findWithSubjectByCertificateId(certificateId.get());
        Map<Long, Boolean> latestCorrect = records.findLatestPerQuestion(memberId).stream()
                .collect(Collectors.toMap(LatestSolve::getQuestionId, LatestSolve::getCorrect));
        LocalDate today = LocalDate.now(KST);
        Set<Long> solvedToday = records.findQuestionIdsSolvedSince(memberId, today.atStartOfDay(KST).toInstant());
        Set<Integer> riskySubjects = reports.reportFor(memberId, certificateId.get()).stream()
                .filter(SubjectReportService.SubjectReport::atRisk)
                .map(SubjectReportService.SubjectReport::subjectNumber)
                .collect(Collectors.toSet());

        Random random = new Random(Objects.hash(memberId, today));
        Predicate<Question> unsolved = q -> !latestCorrect.containsKey(q.getId());
        Set<Question> picked = new LinkedHashSet<>(); // 넣은 순서 유지 + 중복 제거
        fill(picked, pick(all, q -> Boolean.FALSE.equals(latestCorrect.get(q.getId()))
                && !solvedToday.contains(q.getId()), random), 8);
        fill(picked, pick(all, unsolved.and(q -> riskySubjects.contains(q.getSubject().getNumber())), random), 6);
        fill(picked, pick(all, unsolved, random), LIMIT);
        fill(picked, pick(all, unsolved.negate(), random), LIMIT);
        return assembler.toViews(memberId, new ArrayList<>(picked));
    }

    private static List<Question> pick(List<Question> all, Predicate<Question> condition, Random random) {
        List<Question> list = new ArrayList<>(all.stream().filter(condition).toList());
        Collections.shuffle(list, random);
        return list;
    }

    private static void fill(Set<Question> picked, List<Question> candidates, int maxFromBucket) {
        int added = 0;
        for (Question q : candidates) {
            if (picked.size() >= LIMIT || added >= maxFromBucket) {
                return;
            }
            if (picked.add(q)) {
                added++;
            }
        }
    }
}
