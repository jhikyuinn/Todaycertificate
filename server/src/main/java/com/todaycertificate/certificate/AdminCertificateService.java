package com.todaycertificate.certificate;

import com.todaycertificate.common.ApiException;
import com.todaycertificate.common.ErrorCode;
import com.todaycertificate.exam.QuestionRepository;
import com.todaycertificate.member.MemberRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminCertificateService {

    private final CertificateRepository certificates;
    private final SubjectRepository subjects;
    private final ExamScheduleRepository schedules;
    private final QuestionRepository questions;
    private final MemberRepository members;

    public AdminCertificateService(CertificateRepository certificates, SubjectRepository subjects,
                                   ExamScheduleRepository schedules, QuestionRepository questions,
                                   MemberRepository members) {
        this.certificates = certificates;
        this.subjects = subjects;
        this.schedules = schedules;
        this.questions = questions;
        this.members = members;
    }

    @Transactional
    public Long create(CertificateRequest req) {
        validateSubjectNumbers(req);
        if (certificates.existsByName(req.name())) {
            throw new ApiException(ErrorCode.DUPLICATE_NAME);
        }
        Certificate certificate = new Certificate(req.name(), req.description(), req.timeLimitMinutes(),
                req.failScore(), req.passAverage());
        try {
            certificates.saveAndFlush(certificate);
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(ErrorCode.DUPLICATE_NAME); // 동시에 같은 이름으로 만든 경우
        }
        req.subjects().forEach(s -> subjects.save(new Subject(certificate, s.number(), s.name())));
        return certificate.getId();
    }

    /**
     * 과목은 번호 기준으로 맞춘다: 같은 번호는 이름 수정, 새 번호는 추가, 빠진 번호는 삭제.
     * 문제가 달린 과목을 빼려 하면 아무것도 바꾸기 전에 409를 낸다.
     */
    @Transactional
    public void update(Long id, CertificateRequest req) {
        Certificate certificate = certificates.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        validateSubjectNumbers(req);
        if (certificates.existsByNameAndIdNot(req.name(), id)) {
            throw new ApiException(ErrorCode.DUPLICATE_NAME);
        }

        Map<Integer, CertificateRequest.SubjectRequest> wanted = req.subjects().stream()
                .collect(Collectors.toMap(CertificateRequest.SubjectRequest::number, Function.identity()));
        List<Subject> current = subjects.findByCertificateIdOrderByNumber(id);
        List<Subject> removed = current.stream().filter(s -> !wanted.containsKey(s.getNumber())).toList();
        if (!removed.isEmpty() && questions.existsBySubjectIdIn(removed.stream().map(Subject::getId).toList())) {
            throw new ApiException(ErrorCode.SUBJECT_IN_USE);
        }

        // 여기서부터 변경. 엔티티 값만 바꾸면 트랜잭션이 끝날 때 JPA가 UPDATE를 만든다 (변경 감지)
        certificate.update(req.name(), req.description(), req.timeLimitMinutes(), req.failScore(), req.passAverage());
        Set<Integer> existingNumbers = new HashSet<>();
        for (Subject subject : current) {
            existingNumbers.add(subject.getNumber());
            if (wanted.containsKey(subject.getNumber())) {
                subject.rename(wanted.get(subject.getNumber()).name());
            }
        }
        subjects.deleteAll(removed);
        wanted.values().stream()
                .filter(s -> !existingNumbers.contains(s.number()))
                .forEach(s -> subjects.save(new Subject(certificate, s.number(), s.name())));
    }

    @Transactional
    public Long addSchedule(Long certificateId, ScheduleRequest req) {
        Certificate certificate = certificates.findById(certificateId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        if (req.registrationStart().isAfter(req.registrationEnd()) || req.registrationEnd().isAfter(req.examDate())) {
            throw invalid("registrationEnd", "접수 시작일 ≤ 접수 마감일 ≤ 시험일 순서여야 합니다.");
        }
        ExamSchedule schedule = new ExamSchedule(certificate, req.year(), req.round(),
                req.registrationStart(), req.registrationEnd(), req.examDate());
        return schedules.save(schedule).getId();
    }

    @Transactional
    public void deleteSchedule(Long scheduleId) {
        ExamSchedule schedule = schedules.findById(scheduleId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        members.clearTargetSchedule(scheduleId);
        schedules.delete(schedule);
    }

    private static void validateSubjectNumbers(CertificateRequest req) {
        Set<Integer> seen = new HashSet<>();
        for (CertificateRequest.SubjectRequest s : req.subjects()) {
            if (!seen.add(s.number())) {
                throw invalid("subjects", "과목 번호 " + s.number() + "이(가) 중복됩니다.");
            }
        }
    }

    private static ApiException invalid(String field, String message) {
        return new ApiException(ErrorCode.VALIDATION_FAILED, List.of(Map.of("field", field, "message", message)));
    }
}
