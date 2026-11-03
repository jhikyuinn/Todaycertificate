package com.todaycertificate.exam;

import com.todaycertificate.certificate.Certificate;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

/** 기출 회차. 예: 정보처리기사 2025년 1회 */
@Entity
public class Exam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Certificate certificate;

    private int year;
    private int round;

    protected Exam() {
    }

    public Exam(Certificate certificate, int year, int round) {
        this.certificate = certificate;
        this.year = year;
        this.round = round;
    }

    public Long getId() { return id; }
    public Certificate getCertificate() { return certificate; }
    public int getYear() { return year; }
    public int getRound() { return round; }
}
