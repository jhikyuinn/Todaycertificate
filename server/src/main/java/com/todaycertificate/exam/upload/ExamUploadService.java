package com.todaycertificate.exam.upload;

import com.todaycertificate.certificate.Certificate;
import com.todaycertificate.certificate.CertificateRepository;
import com.todaycertificate.certificate.Subject;
import com.todaycertificate.certificate.SubjectRepository;
import com.todaycertificate.common.ApiException;
import com.todaycertificate.common.ErrorCode;
import com.todaycertificate.exam.Exam;
import com.todaycertificate.exam.ExamRepository;
import com.todaycertificate.exam.Question;
import com.todaycertificate.exam.QuestionRepository;
import com.todaycertificate.solve.BookmarkRepository;
import com.todaycertificate.solve.SolveRecordRepository;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExamUploadService {

    private final CertificateRepository certificates;
    private final SubjectRepository subjects;
    private final ExamRepository exams;
    private final QuestionRepository questions;
    private final SolveRecordRepository solveRecords;
    private final BookmarkRepository bookmarks;

    public ExamUploadService(CertificateRepository certificates, SubjectRepository subjects, ExamRepository exams,
                             QuestionRepository questions, SolveRecordRepository solveRecords,
                             BookmarkRepository bookmarks) {
        this.certificates = certificates;
        this.subjects = subjects;
        this.exams = exams;
        this.questions = questions;
        this.solveRecords = solveRecords;
        this.bookmarks = bookmarks;
    }

    public record UploadResult(Long examId, int created, int updated, int deleted) {
    }

    /**
     * 전체가 트랜잭션 하나다. CSV에 오류가 하나라도 있으면 아무것도 저장하지 않는다.
     * 같은 회차를 다시 올리면 문제번호 기준으로 덮어써서 사용자의 풀이 기록과 별표를 유지한다.
     */
    @Transactional
    public UploadResult upload(Long certificateId, int year, int round, byte[] csv) {
        Certificate certificate = certificates.findById(certificateId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        if (year < 2000 || year > 2100 || round < 1 || round > 10) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED,
                    List.of(Map.of("field", "year/round", "message", "연도는 2000~2100, 회차는 1~10이어야 합니다.")));
        }
        Map<Integer, Subject> subjectByNumber = subjects.findByCertificateIdOrderByNumber(certificateId).stream()
                .collect(Collectors.toMap(Subject::getNumber, Function.identity()));

        CsvParseResult parsed = CsvQuestionParser.parse(csv, subjectByNumber.keySet());
        if (!parsed.errors().isEmpty()) {
            throw new ApiException(ErrorCode.CSV_INVALID, parsed.errors());
        }

        Exam exam = exams.findByCertificateIdAndYearAndRound(certificateId, year, round)
                .orElseGet(() -> exams.save(new Exam(certificate, year, round)));
        Map<Integer, Question> existing = questions.findByExamId(exam.getId()).stream()
                .collect(Collectors.toMap(Question::getNumber, Function.identity()));

        int created = 0;
        int updated = 0;
        for (QuestionRow row : parsed.rows()) {
            Subject subject = subjectByNumber.get(row.subjectNumber());
            Question question = existing.remove(row.number());
            if (question != null) {
                question.update(subject, row.content(), row.choices(), row.answerNo(), row.explanation());
                updated++;
            } else {
                questions.save(new Question(exam, subject, row.number(), row.content(), row.choices(),
                        row.answerNo(), row.explanation()));
                created++;
            }
        }

        // 남은 것 = 새 파일에 없는 문제번호. 이 문제를 가리키는 기록을 먼저 지워야 외래키에 막히지 않는다
        Set<Long> removedIds = existing.values().stream().map(Question::getId).collect(Collectors.toSet());
        if (!removedIds.isEmpty()) {
            solveRecords.deleteByQuestionIds(removedIds);
            bookmarks.deleteByQuestionIds(removedIds);
            questions.deleteAll(existing.values());
        }
        return new UploadResult(exam.getId(), created, updated, removedIds.size());
    }
}
