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

    private Integer selectedNo; // 객관식 기록만
    private String answerText;  // 주관식 기록만
    private boolean correct;

    @Enumerated(EnumType.STRING)
    private SolveMode mode;

    private Instant solvedAt;

    protected SolveRecord() {
    }

    /**
     * attempt는 시험 모드일 때만 넘긴다. selectedNo(객관식)와 answerText(주관식) 중 하나만 넘긴다
     * (호출 전에 Question.invalidAnswerField로 검사). 정답 여부는 문제 유형에 맞게 계산한다.
     */
    public SolveRecord(Member member, Question question, ExamAttempt attempt, Integer selectedNo, String answerText,
                       SolveMode mode, Instant solvedAt) {
        this.member = member;
        this.question = question;
        this.attempt = attempt;
        this.selectedNo = selectedNo;
        this.answerText = answerText;
        this.correct = question.isCorrect(selectedNo, answerText);
        this.mode = mode;
        this.solvedAt = solvedAt;
    }

    public Long getId() { return id; }
    public Member getMember() { return member; }
    public Question getQuestion() { return question; }
    public ExamAttempt getAttempt() { return attempt; }
    public Integer getSelectedNo() { return selectedNo; }
    public String getAnswerText() { return answerText; }
    public boolean isCorrect() { return correct; }
    public SolveMode getMode() { return mode; }
    public Instant getSolvedAt() { return solvedAt; }
}
