package com.todaycertificate;

import com.todaycertificate.member.AuthService;
import java.util.ArrayList;
import java.util.List;
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

    public record Seed(Long certificateId, Long examId, List<Long> questionIds) {
    }

    /**
     * 과목 2개짜리 자격증, 2025년 1회 시험, 과목마다 perSubject개 문제를 넣는다.
     * 문제번호는 1부터, 앞 절반이 1과목. 정답은 모두 1번, 해설은 "해설{번호}".
     */
    public Seed seedExam(String certificateName, int perSubject) {
        Long certId = jdbc.queryForObject(
                "INSERT INTO certificate (name, time_limit_minutes) VALUES (?, 60) RETURNING id", Long.class,
                certificateName);
        Long s1 = jdbc.queryForObject(
                "INSERT INTO subject (certificate_id, number, name) VALUES (?, 1, '1과목') RETURNING id", Long.class,
                certId);
        Long s2 = jdbc.queryForObject(
                "INSERT INTO subject (certificate_id, number, name) VALUES (?, 2, '2과목') RETURNING id", Long.class,
                certId);
        Long examId = jdbc.queryForObject(
                "INSERT INTO exam (certificate_id, year, round) VALUES (?, 2025, 1) RETURNING id", Long.class, certId);
        List<Long> ids = new ArrayList<>();
        for (int n = 1; n <= perSubject * 2; n++) {
            ids.add(jdbc.queryForObject("""
                    INSERT INTO question (exam_id, subject_id, number, content, choice1, choice2, choice3, choice4,
                                          answer_no, explanation)
                    VALUES (?, ?, ?, ?, 'a', 'b', 'c', 'd', 1, ?) RETURNING id""", Long.class,
                    examId, n <= perSubject ? s1 : s2, n, "문제" + n, "해설" + n));
        }
        return new Seed(certId, examId, ids);
    }
}
