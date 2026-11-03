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

    public Long getId() { return id; }
    public Member getMember() { return member; }
    public Exam getExam() { return exam; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getSubmittedAt() { return submittedAt; }
    public Double getAverageScore() { return averageScore; }
    public Boolean getPassed() { return passed; }
}
