package com.todaycertificate.study;

import com.todaycertificate.certificate.Certificate;
import com.todaycertificate.certificate.CertificateRepository;
import com.todaycertificate.certificate.Subject;
import com.todaycertificate.certificate.SubjectRepository;
import com.todaycertificate.exam.Question;
import com.todaycertificate.exam.QuestionRepository;
import com.todaycertificate.solve.SolveRecordRepository;
import com.todaycertificate.solve.SolveRecordRepository.LatestSolve;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 과목별 과락 위험. 문제마다 가장 최근 풀이만 센다 (예전에 틀렸어도 지금 맞히면 실력으로 본다).
 * 5문제 미만으로 푼 과목은 표본이 적어서 위험 판단을 보류한다.
 */
@Service
public class SubjectReportService {

    static final int MIN_SOLVED = 5;

    private final StudyTarget target;
    private final CertificateRepository certificates;
    private final SubjectRepository subjects;
    private final QuestionRepository questions;
    private final SolveRecordRepository records;

    public SubjectReportService(StudyTarget target, CertificateRepository certificates, SubjectRepository subjects,
                                QuestionRepository questions, SolveRecordRepository records) {
        this.target = target;
        this.certificates = certificates;
        this.subjects = subjects;
        this.questions = questions;
        this.records = records;
    }

    public record SubjectReport(int subjectNumber, String subjectName, int solvedCount, int correctRate,
                                boolean atRisk) {
    }

    @Transactional(readOnly = true)
    public List<SubjectReport> report(Long memberId) {
        return target.certificateId(memberId).map(id -> reportFor(memberId, id)).orElse(List.of());
    }

    /** 오늘의 문제(#20)도 과락 위험 과목을 고를 때 쓴다. 호출하는 쪽 트랜잭션 안에서 실행된다. */
    List<SubjectReport> reportFor(Long memberId, Long certificateId) {
        Certificate certificate = certificates.findById(certificateId).orElseThrow();
        Map<Long, Boolean> latestCorrect = records.findLatestPerQuestion(memberId).stream()
                .collect(Collectors.toMap(LatestSolve::getQuestionId, LatestSolve::getCorrect));

        // 과목 ID → {맞힌 수, 푼 수}. getSubject().getId()는 프록시에서 ID만 꺼내므로 추가 조회가 없다
        Map<Long, int[]> counts = new HashMap<>();
        for (Question q : questions.findAllById(latestCorrect.keySet())) {
            int[] c = counts.computeIfAbsent(q.getSubject().getId(), k -> new int[2]);
            if (latestCorrect.get(q.getId())) {
                c[0]++;
            }
            c[1]++;
        }

        return subjects.findByCertificateIdOrderByNumber(certificateId).stream().map((Subject s) -> {
            int[] c = counts.getOrDefault(s.getId(), new int[2]);
            int rate = c[1] == 0 ? 0 : c[0] * 100 / c[1];
            return new SubjectReport(s.getNumber(), s.getName(), c[1], rate,
                    c[1] >= MIN_SOLVED && rate < certificate.getFailScore());
        }).toList();
    }
}
