package com.todaycertificate.certificate;

import com.todaycertificate.common.ApiException;
import com.todaycertificate.common.ErrorCode;
import com.todaycertificate.exam.ExamRepository;
import com.todaycertificate.exam.ExamRepository.ExamSummary;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** 로그인한 사용자용 자격증 조회. 조회만 하고 지연 로딩을 타지 않으므로 Repository를 바로 쓴다. */
@RestController
public class CertificateController {

    static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final CertificateRepository certificates;
    private final SubjectRepository subjects;
    private final ExamScheduleRepository schedules;
    private final ExamRepository exams;

    public CertificateController(CertificateRepository certificates, SubjectRepository subjects,
                                 ExamScheduleRepository schedules, ExamRepository exams) {
        this.certificates = certificates;
        this.subjects = subjects;
        this.schedules = schedules;
        this.exams = exams;
    }

    record CertificateItem(Long id, String name, String description) {
    }

    record SubjectItem(int number, String name) {
    }

    record ScheduleItem(Long id, int year, int round, LocalDate registrationStart, LocalDate registrationEnd,
                        LocalDate examDate) {
    }

    record CertificateDetail(Long id, String name, String description, int timeLimitMinutes, int failScore,
                             int passAverage, List<SubjectItem> subjects, List<ScheduleItem> schedules,
                             List<ExamSummary> exams) {
    }

    @GetMapping("/api/certificates")
    List<CertificateItem> list() {
        return certificates.findAll(Sort.by("name")).stream()
                .map(c -> new CertificateItem(c.getId(), c.getName(), c.getDescription()))
                .toList();
    }

    @GetMapping("/api/certificates/{id}")
    CertificateDetail detail(@PathVariable Long id) {
        Certificate c = certificates.findById(id).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        int thisYear = LocalDate.now(KST).getYear(); // 서버가 UTC여도 한국 기준 올해
        return new CertificateDetail(c.getId(), c.getName(), c.getDescription(), c.getTimeLimitMinutes(),
                c.getFailScore(), c.getPassAverage(),
                subjects.findByCertificateIdOrderByNumber(id).stream()
                        .map(s -> new SubjectItem(s.getNumber(), s.getName())).toList(),
                schedules.findByCertificateIdAndYearOrderByExamDate(id, thisYear).stream()
                        .map(s -> new ScheduleItem(s.getId(), s.getYear(), s.getRound(), s.getRegistrationStart(),
                                s.getRegistrationEnd(), s.getExamDate())).toList(),
                exams.findSummaries(id));
    }
}
