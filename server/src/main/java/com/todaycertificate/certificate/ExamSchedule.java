package com.todaycertificate.certificate;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import java.time.LocalDate;

@Entity
public class ExamSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Certificate certificate;

    private int year;
    private int round;
    private LocalDate registrationStart;
    private LocalDate registrationEnd;
    private LocalDate examDate;

    protected ExamSchedule() {
    }

    public Long getId() { return id; }
    public Certificate getCertificate() { return certificate; }
    public int getYear() { return year; }
    public int getRound() { return round; }
    public LocalDate getRegistrationStart() { return registrationStart; }
    public LocalDate getRegistrationEnd() { return registrationEnd; }
    public LocalDate getExamDate() { return examDate; }
}
