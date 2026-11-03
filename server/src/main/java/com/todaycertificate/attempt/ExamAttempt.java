package com.todaycertificate.attempt;

import com.todaycertificate.exam.Exam;
import com.todaycertificate.member.Member;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import java.time.Instant;

/** 시험 모드 응시 한 번. 제출 전에는 submittedAt, averageScore, passed가 null이다. */
@Entity
public class ExamAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    private Exam exam;

    private Instant startedAt;
    private Instant submittedAt;
    private Double averageScore;
    private Boolean passed;

    protected ExamAttempt() {
    }

    public ExamAttempt(Member member, Exam exam, Instant startedAt) {
        this.member = member;
        this.exam = exam;
        this.startedAt = startedAt;
    }

    public boolean isOwnedBy(Long memberId) {
        return member.getId().equals(memberId);
    }

    public boolean isSubmitted() {
        return submittedAt != null;
    }

    /**
     * 채점 결과를 기록한다. submittedAt도 함께 넣는 이유: 제출 잠금은 UPDATE 쿼리로 먼저 걸지만,
     * 이 엔티티는 그 전에 읽어서 submittedAt이 null인 상태다. JPA는 변경 감지 때 모든 컬럼을 UPDATE하므로
     * 여기서 넣지 않으면 잠금으로 기록한 제출 시각을 null로 덮어쓴다.
     */
    public void recordResult(Instant submittedAt, double averageScore, boolean passed) {
        this.submittedAt = submittedAt;
        this.averageScore = averageScore;
        this.passed = passed;
    }

    public Long getId() { return id; }
    public Member getMember() { return member; }
    public Exam getExam() { return exam; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getSubmittedAt() { return submittedAt; }
    public Double getAverageScore() { return averageScore; }
    public Boolean getPassed() { return passed; }
}
