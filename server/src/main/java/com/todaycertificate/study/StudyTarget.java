package com.todaycertificate.study;

import com.todaycertificate.member.Member;
import com.todaycertificate.member.MemberRepository;
import com.todaycertificate.solve.SolveRecordRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * 오늘의 문제(#20)와 과락 리포트(#22)가 어떤 자격증을 대상으로 할지 정한다.
 * D-day 목표 자격증 → 없으면 가장 최근에 푼 문제의 자격증 → 그것도 없으면 없음.
 * 지연 로딩을 타므로 트랜잭션 안에서 호출해야 한다.
 */
@Component
public class StudyTarget {

    private final MemberRepository members;
    private final SolveRecordRepository records;

    public StudyTarget(MemberRepository members, SolveRecordRepository records) {
        this.members = members;
        this.records = records;
    }

    public Optional<Long> certificateId(Long memberId) {
        Member member = members.findById(memberId).orElseThrow();
        if (member.getTargetSchedule() != null) {
            return Optional.of(member.getTargetSchedule().getCertificate().getId());
        }
        return records.findLastSolvedCertificateId(memberId);
    }
}
