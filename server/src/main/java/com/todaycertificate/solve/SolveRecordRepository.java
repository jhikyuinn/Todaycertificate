package com.todaycertificate.solve;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SolveRecordRepository extends JpaRepository<SolveRecord, Long> {

    @Modifying
    @Query("DELETE FROM SolveRecord r WHERE r.question.id IN :questionIds")
    int deleteByQuestionIds(@Param("questionIds") Collection<Long> questionIds);

    List<SolveRecord> findByAttemptId(Long attemptId);

    interface LatestSolve {
        Long getQuestionId();

        boolean getCorrect();
    }

    /**
     * 문제마다 그 회원의 가장 최근 풀이 하나. 오답노트(#15)와 과락 리포트(#22)가 함께 쓴다.
     * PostgreSQL의 DISTINCT ON: question_id별로 ORDER BY 첫 행만 남긴다. 같은 시각이면 id가 큰 쪽.
     */
    @Query(value = """
            SELECT DISTINCT ON (question_id) question_id AS questionId, correct
            FROM solve_record
            WHERE member_id = :memberId
            ORDER BY question_id, solved_at DESC, id DESC""", nativeQuery = true)
    List<LatestSolve> findLatestPerQuestion(@Param("memberId") Long memberId);

    // 가장 최근에 푼 문제의 자격증 (오늘의 문제·과락 리포트의 대상 자격증 후보)
    @Query(value = """
            SELECT e.certificate_id FROM solve_record r
            JOIN question q ON q.id = r.question_id JOIN exam e ON e.id = q.exam_id
            WHERE r.member_id = :memberId
            ORDER BY r.solved_at DESC, r.id DESC LIMIT 1""", nativeQuery = true)
    Optional<Long> findLastSolvedCertificateId(@Param("memberId") Long memberId);
}
