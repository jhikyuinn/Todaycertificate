package com.todaycertificate.certificate;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
public class Subject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Certificate certificate;

    private int number; // 1과목, 2과목 …
    private String name;

    protected Subject() {
    }

    public Subject(Certificate certificate, int number, String name) {
        this.certificate = certificate;
        this.number = number;
        this.name = name;
    }

    public void rename(String name) {
        this.name = name;
    }

    public Long getId() { return id; }
    public Certificate getCertificate() { return certificate; }
    public int getNumber() { return number; }
    public String getName() { return name; }
}
