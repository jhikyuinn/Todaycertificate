package com.todaycertificate;

import com.todaycertificate.member.AuthService;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.jdbc.core.JdbcTemplate;

/** 테스트 공통 도우미: DB 비우기, 일반 회원과 관리자 토큰 만들기. */
@TestComponent
public class TestFixtures {

    private final JdbcTemplate jdbc;
    private final AuthService authService;

    public TestFixtures(JdbcTemplate jdbc, AuthService authService) {
        this.jdbc = jdbc;
        this.authService = authService;
    }

    public void cleanAll() {
        jdbc.execute("""
                TRUNCATE solve_record, bookmark, exam_attempt, question, exam,
                         member, exam_schedule, subject, certificate RESTART IDENTITY CASCADE""");
    }

    public String userToken(String loginId) {
        authService.signup(loginId, "테스터", "password123");
        return authService.login(loginId, "password123").accessToken();
    }

    public String adminToken(String loginId) {
        authService.signup(loginId, "관리자", "password123");
        jdbc.update("UPDATE member SET role = 'ADMIN' WHERE login_id = ?", loginId);
        return authService.login(loginId, "password123").accessToken();
    }

    public Long memberId(String loginId) {
        return jdbc.queryForObject("SELECT id FROM member WHERE login_id = ?", Long.class, loginId);
    }
}
