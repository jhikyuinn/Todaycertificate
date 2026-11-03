package com.todaycertificate.solve;

import com.todaycertificate.exam.Question;
import com.todaycertificate.member.Member;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

/** 별표. 회원과 문제 한 쌍에 하나만 있다 (DB UNIQUE 제약). */
@Entity
public class Bookmark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    private Question question;

    protected Bookmark() {
    }

    public Long getId() { return id; }
    public Member getMember() { return member; }
    public Question getQuestion() { return question; }
}
