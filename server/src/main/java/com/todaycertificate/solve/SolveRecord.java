package com.todaycertificate.solve;

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

    private int selectedNo;
    private boolean correct;

    @Enumerated(EnumType.STRING)
    private SolveMode mode;

    private Instant solvedAt;

    protected SolveRecord() {
    }

    public Long getId() { return id; }
    public Member getMember() { return member; }
    public Question getQuestion() { return question; }
    public int getSelectedNo() { return selectedNo; }
    public boolean isCorrect() { return correct; }
    public SolveMode getMode() { return mode; }
    public Instant getSolvedAt() { return solvedAt; }
}
