package com.todaycertificate.attempt;

import java.time.Instant;
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
}
