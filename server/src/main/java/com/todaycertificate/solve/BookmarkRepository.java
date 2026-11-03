package com.todaycertificate.solve;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    // 이미 있으면 아무것도 하지 않는다: 동시에 두 번 눌러도 UNIQUE 위반(500) 없이 멱등
    @Modifying
    @Query(value = "INSERT INTO bookmark (member_id, question_id) VALUES (:memberId, :questionId) ON CONFLICT DO NOTHING",
            nativeQuery = true)
    int insertIfAbsent(@Param("memberId") Long memberId, @Param("questionId") Long questionId);

    @Modifying
    @Query("DELETE FROM Bookmark b WHERE b.member.id = :memberId AND b.question.id = :questionId")
    int deleteByMemberAndQuestion(@Param("memberId") Long memberId, @Param("questionId") Long questionId);

    @Modifying
    @Query("DELETE FROM Bookmark b WHERE b.question.id IN :questionIds")
    int deleteByQuestionIds(@Param("questionIds") Collection<Long> questionIds);

    // 문제 목록 중 내가 별표한 것만: 문제마다 따로 조회하지 않고 한 번에 (N+1 방지)
    @Query("SELECT b.question.id FROM Bookmark b WHERE b.member.id = :memberId AND b.question.id IN :questionIds")
    Set<Long> findBookmarkedQuestionIds(@Param("memberId") Long memberId,
                                       @Param("questionIds") Collection<Long> questionIds);

    @Query("SELECT b.question.id FROM Bookmark b WHERE b.member.id = :memberId ORDER BY b.id DESC")
    List<Long> findQuestionIdsByMemberId(@Param("memberId") Long memberId);
}
