package com.todaycertificate.exam;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    boolean existsBySubjectIdIn(Collection<Long> subjectIds);

    List<Question> findByExamId(Long examId);

    // 과목을 fetch join으로 함께 가져온다: 문제마다 과목을 따로 조회하는 N+1을 막는다
    @Query("SELECT q FROM Question q JOIN FETCH q.subject WHERE q.exam.id = :examId ORDER BY q.number")
    List<Question> findWithSubjectByExamId(@Param("examId") Long examId);

    // 오답노트·별표 목록용: 문제, 과목, 회차, 자격증을 한 번에
    @Query("""
            SELECT q FROM Question q
            JOIN FETCH q.subject JOIN FETCH q.exam e JOIN FETCH e.certificate
            WHERE q.id IN :ids""")
    List<Question> findWithDetailsByIdIn(@Param("ids") Collection<Long> ids);
}
