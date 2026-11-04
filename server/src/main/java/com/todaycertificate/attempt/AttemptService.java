package com.todaycertificate.attempt;

import com.todaycertificate.attempt.ScoreCalculator.ExamScore;
import com.todaycertificate.attempt.ScoreCalculator.GradedQuestion;
import com.todaycertificate.attempt.ScoreCalculator.SubjectScore;
import com.todaycertificate.certificate.Certificate;
import com.todaycertificate.certificate.CertificateRepository;
import com.todaycertificate.common.ApiException;
import com.todaycertificate.common.ErrorCode;
import com.todaycertificate.exam.Exam;
import com.todaycertificate.exam.ExamRepository;
import com.todaycertificate.exam.Question;
import com.todaycertificate.exam.QuestionRepository;
import com.todaycertificate.exam.QuestionType;
import com.todaycertificate.exam.QuestionView;
import com.todaycertificate.exam.QuestionViewAssembler;
import com.todaycertificate.member.Member;
import com.todaycertificate.member.MemberRepository;
import com.todaycertificate.solve.QuestionStatsService;
import com.todaycertificate.solve.SolveMode;
import com.todaycertificate.solve.SolveRecord;
import com.todaycertificate.solve.SolveRecordRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AttemptService {

    private final ExamAttemptRepository attempts;
    private final ExamRepository exams;
    private final QuestionRepository questions;
    private final MemberRepository members;
    private final SolveRecordRepository records;
    private final QuestionViewAssembler assembler;
    private final QuestionStatsService stats;
    private final CertificateRepository certificates;

    public AttemptService(ExamAttemptRepository attempts, ExamRepository exams, QuestionRepository questions,
                          MemberRepository members, SolveRecordRepository records, QuestionViewAssembler assembler,
                          QuestionStatsService stats, CertificateRepository certificates) {
        this.attempts = attempts;
        this.exams = exams;
        this.questions = questions;
        this.members = members;
        this.records = records;
        this.assembler = assembler;
        this.stats = stats;
        this.certificates = certificates;
    }

    /** 객관식은 selectedNo, 주관식은 answerText 하나만 채운다. */
    public record Answer(Long questionId, Integer selectedNo, String answerText) {
    }

    public record StartResult(Long attemptId, Instant startedAt, int timeLimitMinutes, List<QuestionView> questions) {
    }

    /**
     * 내 답은 selectedNo(객관식) 또는 answerText(주관식), 정답은 answerNo 또는 correctAnswerText.
     * 내 답이 둘 다 null이면 안 푼 문제 (오답 처리). stats는 전체 회원 통계, 표본이 적으면 null.
     */
    public record QuestionResult(Long questionId, int number, QuestionType type, Integer selectedNo, String answerText,
                                 Integer answerNo, String correctAnswerText, boolean correct, String explanation,
                                 QuestionStatsService.Stats stats) {
    }

    public record AttemptResult(Long attemptId, double averageScore, boolean passed, List<SubjectScore> subjects,
                                List<QuestionResult> questions) {
    }

    @Transactional
    public StartResult start(Long memberId, Long examId) {
        Exam exam = exams.findById(examId).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        List<Question> list = questions.findWithSubjectByExamId(examId);
        ExamAttempt attempt = attempts.save(new ExamAttempt(members.getReferenceById(memberId), exam, Instant.now()));
        // shortcut: 제한시간은 클라이언트 타이머가 자동 제출로 지킨다. 서버 측 시간 초과 거부가 필요해지면 submit에서 startedAt과 비교
        return new StartResult(attempt.getId(), attempt.getStartedAt(), exam.getCertificate().getTimeLimitMinutes(),
                assembler.toViews(memberId, list));
    }

    @Transactional
    public AttemptResult submit(Long memberId, Long attemptId, List<Answer> answers) {
        ExamAttempt attempt = findOwned(memberId, attemptId);
        List<Question> list = questions.findWithSubjectByExamId(attempt.getExam().getId());
        Map<Long, Question> byId = list.stream().collect(Collectors.toMap(Question::getId, q -> q));

        // 검증을 잠금보다 먼저: 잘못된 요청이 응시를 "제출됨"으로 만들지 않게
        Map<Long, Answer> selected = new HashMap<>();
        for (Answer a : answers) {
            if (!byId.containsKey(a.questionId()) || selected.put(a.questionId(), a) != null) {
                throw new ApiException(ErrorCode.VALIDATION_FAILED, List.of(Map.of("field", "answers",
                        "message", "이 시험에 없는 문제이거나 같은 문제를 두 번 보냈습니다: " + a.questionId())));
            }
            String invalid = byId.get(a.questionId()).invalidAnswerField(a.selectedNo(), a.answerText());
            if (invalid != null) {
                throw new ApiException(ErrorCode.VALIDATION_FAILED, List.of(Map.of("field", invalid,
                        "message", "객관식은 selectedNo, 주관식은 answerText 하나만 보내야 합니다: " + a.questionId())));
            }
        }

        Instant now = Instant.now();
        if (attempts.markSubmitted(attemptId, now) == 0) {
            throw new ApiException(ErrorCode.ALREADY_SUBMITTED);
        }

        Member member = members.getReferenceById(memberId);
        records.saveAll(selected.values().stream()
                .map(a -> new SolveRecord(member, byId.get(a.questionId()), attempt, a.selectedNo(), a.answerText(),
                        SolveMode.EXAM, now))
                .toList());

        AttemptResult result = grade(attempt, list, selected);
        attempt.recordResult(now, result.averageScore(), result.passed());
        return result;
    }

    /** 제출한 시험의 결과를 다시 본다. 저장된 풀이 기록으로 다시 채점하므로 제출 응답과 같은 모양이다. */
    @Transactional(readOnly = true)
    public AttemptResult result(Long memberId, Long attemptId) {
        ExamAttempt attempt = findOwned(memberId, attemptId);
        if (!attempt.isSubmitted()) {
            throw new ApiException(ErrorCode.NOT_FOUND);
        }
        Map<Long, Answer> selected = records.findByAttemptId(attemptId).stream()
                .map(r -> new Answer(r.getQuestion().getId(), r.getSelectedNo(), r.getAnswerText()))
                .collect(Collectors.toMap(Answer::questionId, a -> a));
        return grade(attempt, questions.findWithSubjectByExamId(attempt.getExam().getId()), selected);
    }

    public record HistoryItem(Long attemptId, int examYear, int examRound, Instant submittedAt, double averageScore,
                              boolean passed) {
    }

    /** 점수 이력. 제출 때 저장한 점수를 그대로 쓴다 (다시 채점하지 않음). */
    @Transactional(readOnly = true)
    public List<HistoryItem> history(Long memberId, Long certificateId) {
        if (!certificates.existsById(certificateId)) {
            throw new ApiException(ErrorCode.NOT_FOUND);
        }
        return attempts.findSubmittedHistory(memberId, certificateId).stream()
                .map(a -> new HistoryItem(a.getId(), a.getExam().getYear(), a.getExam().getRound(), a.getSubmittedAt(),
                        a.getAverageScore(), a.getPassed()))
                .toList();
    }

    private ExamAttempt findOwned(Long memberId, Long attemptId) {
        ExamAttempt attempt = attempts.findById(attemptId).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        if (!attempt.isOwnedBy(memberId)) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }
        return attempt;
    }

    private AttemptResult grade(ExamAttempt attempt, List<Question> list, Map<Long, Answer> selected) {
        Map<Long, QuestionStatsService.Stats> statsById = stats.statsFor(list);
        Answer unanswered = new Answer(null, null, null);
        List<QuestionResult> results = list.stream().map(q -> {
            Answer a = selected.getOrDefault(q.getId(), unanswered);
            return new QuestionResult(q.getId(), q.getNumber(), q.getType(), a.selectedNo(), a.answerText(),
                    q.getAnswerNo(), q.getAnswerText(), q.isCorrect(a.selectedNo(), a.answerText()),
                    q.getExplanation(), statsById.get(q.getId()));
        }).toList();
        Set<Long> correctIds = results.stream().filter(QuestionResult::correct).map(QuestionResult::questionId)
                .collect(Collectors.toSet());

        Certificate c = attempt.getExam().getCertificate();
        ExamScore score = ScoreCalculator.score(list.stream()
                .map(q -> new GradedQuestion(q.getSubject().getNumber(), q.getSubject().getName(),
                        correctIds.contains(q.getId())))
                .toList(), c.getFailScore(), c.getPassAverage());
        return new AttemptResult(attempt.getId(), score.averageScore(), score.passed(), score.subjects(), results);
    }
}
