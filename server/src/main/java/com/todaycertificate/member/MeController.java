package com.todaycertificate.member;

import com.todaycertificate.certificate.ExamSchedule;
import com.todaycertificate.certificate.ExamScheduleRepository;
import com.todaycertificate.common.ApiException;
import com.todaycertificate.common.CurrentMember;
import com.todaycertificate.common.ErrorCode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MeController {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final MemberRepository members;
    private final ExamScheduleRepository schedules;

    public MeController(MemberRepository members, ExamScheduleRepository schedules) {
        this.members = members;
        this.schedules = schedules;
    }

    record Target(Long certificateId, String certificateName, int year, int round, LocalDate examDate, long dDay) {
    }

    record MeResponse(Long id, String loginId, String name, Role role, Target target) {
    }

    record TargetRequest(Long scheduleId) {
    }

    @GetMapping("/api/me")
    @Transactional(readOnly = true)
    MeResponse me(Authentication authentication) {
        Member member = find(authentication);
        ExamSchedule s = member.getTargetSchedule();
        Target target = s == null ? null : new Target(s.getCertificate().getId(), s.getCertificate().getName(),
                s.getYear(), s.getRound(), s.getExamDate(),
                // 한국 날짜 기준 남은 일수. 시험 당일 0, 지나면 음수
                ChronoUnit.DAYS.between(LocalDate.now(KST), s.getExamDate()));
        return new MeResponse(member.getId(), member.getLoginId(), member.getName(), member.getRole(), target);
    }

    /** scheduleId가 null이면 목표 해제. */
    @PutMapping("/api/me/target")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    void changeTarget(@RequestBody TargetRequest request, Authentication authentication) {
        ExamSchedule schedule = request.scheduleId() == null ? null
                : schedules.findById(request.scheduleId()).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        find(authentication).changeTarget(schedule); // 변경 감지로 UPDATE
    }

    private Member find(Authentication authentication) {
        // 토큰은 유효하지만 회원이 삭제된 경우
        return members.findById(CurrentMember.id(authentication))
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED));
    }
}
