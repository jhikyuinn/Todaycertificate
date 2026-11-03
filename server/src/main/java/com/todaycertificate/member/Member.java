package com.todaycertificate.member;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String loginId;
    private String name;
    private String password; // BCrypt 해시만 저장한다
    private String acquiredCertificate;
    private String acquiredCertificateReview;

    protected Member() {
    }

    public Member(String loginId, String name, String encodedPassword) {
        this.loginId = loginId;
        this.name = name;
        this.password = encodedPassword;
    }

    public Long getId() { return id; }
    public String getLoginId() { return loginId; }
    public String getName() { return name; }
    public String getPassword() { return password; }
    public String getAcquiredCertificate() { return acquiredCertificate; }
    public String getAcquiredCertificateReview() { return acquiredCertificateReview; }
}
