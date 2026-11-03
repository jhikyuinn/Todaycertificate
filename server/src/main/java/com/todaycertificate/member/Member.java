package com.todaycertificate.member;

import com.todaycertificate.certificate.ExamSchedule;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String loginId;
    private String name;
    private String password; // BCrypt 해시만 저장한다

    // STRING: DB에 'USER' 같은 이름으로 저장한다. 기본값 ORDINAL(0, 1)은 enum 순서가 바뀌면 데이터가 꼬인다.
    @Enumerated(EnumType.STRING)
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    private ExamSchedule targetSchedule; // D-day 목표. 없으면 null

    protected Member() {
    }

    public Member(String loginId, String name, String encodedPassword) {
        this.loginId = loginId;
        this.name = name;
        this.password = encodedPassword;
        this.role = Role.USER;
    }

    public Long getId() { return id; }
    public String getLoginId() { return loginId; }
    public String getName() { return name; }
    public String getPassword() { return password; }
    public Role getRole() { return role; }
    public ExamSchedule getTargetSchedule() { return targetSchedule; }

    /** null이면 목표 해제. */
    public void changeTarget(ExamSchedule schedule) { this.targetSchedule = schedule; }
}
