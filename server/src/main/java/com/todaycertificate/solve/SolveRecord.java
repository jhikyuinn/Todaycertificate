package com.todaycertificate.solve;

import com.todaycertificate.attempt.ExamAttempt;
import com.todaycertificate.exam.Question;
import com.todaycertificate.member.Member;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import java.time.Instant;

/** 문제 하나를 한 번 푼 기록. 같은 문제를 여러 번 풀면 기록도 여러 개 쌓인다. */
@Entity
public class SolveRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    private Question question;

    @ManyToOne(fetch = FetchType.LAZY)
    private ExamAttempt attempt; // 시험 모드 기록만 채운다. 공부 모드는 null

    private int selectedNo;
    private boolean correct;

    @Enumerated(EnumType.STRING)
    private SolveMode mode;

    private Instant solvedAt;

    protected SolveRecord() {
    }

    /** attempt는 시험 모드일 때만 넘긴다. 정답 여부는 문제의 정답번호로 계산한다. */
    public SolveRecord(Member member, Question question, ExamAttempt attempt, int selectedNo,
                       SolveMode mode, Instant solvedAt) {
        this.member = member;
        this.question = question;
        this.attempt = attempt;
        this.selectedNo = selectedNo;
        this.correct = question.isCorrect(selectedNo);
        this.mode = mode;
        this.solvedAt = solvedAt;
    }

    public Long getId() { return id; }
    public Member getMember() { return member; }
    public Question getQuestion() { return question; }
    public ExamAttempt getAttempt() { return attempt; }
    public int getSelectedNo() { return selectedNo; }
    public boolean isCorrect() { return correct; }
    public SolveMode getMode() { return mode; }
    public Instant getSolvedAt() { return solvedAt; }
}
