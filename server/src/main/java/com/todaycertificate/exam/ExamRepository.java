package com.todaycertificate.exam;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExamRepository extends JpaRepository<Exam, Long> {

    Optional<Exam> findByCertificateIdAndYearAndRound(Long certificateId, int year, int round);

    record ExamSummary(Long id, int year, int round, long questionCount) {
    }

    // 회차 목록과 회차별 문제 수를 쿼리 한 번으로 가져온다 (최신 회차부터)
    @Query("""
            SELECT new com.todaycertificate.exam.ExamRepository$ExamSummary(e.id, e.year, e.round, COUNT(q))
            FROM Exam e LEFT JOIN Question q ON q.exam = e
            WHERE e.certificate.id = :certificateId
            GROUP BY e.id, e.year, e.round
            ORDER BY e.year DESC, e.round DESC""")
    List<ExamSummary> findSummaries(@Param("certificateId") Long certificateId);
}
