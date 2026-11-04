package com.todaycertificate.attempt;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExamAttemptRepository extends JpaRepository<ExamAttempt, Long> {

    /**
     * 제출 잠금. 아직 제출되지 않은 경우에만 제출 시각을 넣고, 바뀐 행 수를 돌려준다.
     * 같은 응시를 동시에 두 번 제출하면 DB가 행 잠금으로 줄을 세우고, 뒤의 요청은 조건이 거짓이 되어 0을 받는다.
     */
    @Modifying
    @Query("UPDATE ExamAttempt a SET a.submittedAt = :now WHERE a.id = :id AND a.submittedAt IS NULL")
    int markSubmitted(@Param("id") Long id, @Param("now") Instant now);

    /** 점수 이력: 내가 제출한 이 자격증의 시험, 제출 오래된 순. exam을 함께 읽어 회차 표시에 추가 쿼리가 없다. */
    @Query("""
            SELECT a FROM ExamAttempt a JOIN FETCH a.exam e
            WHERE a.member.id = :memberId AND e.certificate.id = :certificateId AND a.submittedAt IS NOT NULL
            ORDER BY a.submittedAt""")
    List<ExamAttempt> findSubmittedHistory(@Param("memberId") Long memberId,
                                           @Param("certificateId") Long certificateId);
}
