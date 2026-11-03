package com.todaycertificate.certificate;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamScheduleRepository extends JpaRepository<ExamSchedule, Long> {

    List<ExamSchedule> findByCertificateIdAndYearOrderByExamDate(Long certificateId, int year);
}
