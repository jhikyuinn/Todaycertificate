package com.todaycertificate.certificate;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 문제 여러 개가 자격증 하나에 속한다. LAZY: 자격증 정보가 필요할 때만 조회한다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "certificate_id")
    private Certificate certificate;

    private Integer questionCode;
    private String question;
    private String answer;
    private String explanation;

    protected Question() {
    }

    public Long getId() { return id; }
    public Certificate getCertificate() { return certificate; }
    public Integer getQuestionCode() { return questionCode; }
    public String getQuestion() { return question; }
    public String getAnswer() { return answer; }
    public String getExplanation() { return explanation; }
}
