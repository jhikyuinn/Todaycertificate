package com.todaycertificate.member;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberRepository extends JpaRepository<Member, Long> {

    boolean existsByLoginId(String loginId);

    Optional<Member> findByLoginId(String loginId);

    // 일정을 지우기 전에, 그 일정을 D-day 목표로 둔 회원들의 목표를 비운다 (외래키 때문에 삭제가 막히지 않게)
    @Modifying
    @Query("UPDATE Member m SET m.targetSchedule = null WHERE m.targetSchedule.id = :scheduleId")
    int clearTargetSchedule(@Param("scheduleId") Long scheduleId);
}
