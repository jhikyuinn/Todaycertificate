package com.todaycertificate.certificate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Certificate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String description;
    private int timeLimitMinutes;
    private int failScore;   // 과목 점수가 이 값 미만이면 과락
    private int passAverage; // 평균이 이 값 이상이면 합격

    protected Certificate() {
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getTimeLimitMinutes() { return timeLimitMinutes; }
    public int getFailScore() { return failScore; }
    public int getPassAverage() { return passAverage; }
}
