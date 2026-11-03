package com.todaycertificate.study;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.todaycertificate.TestFixtures;
import com.todaycertificate.TestFixtures.Seed;
import com.todaycertificate.TestcontainersConfig;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** #22 과락 위험 리포트. seed: 과목마다 5문제 (q0~q4 1과목, q5~q9 2과목), 과락 기준 40. */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfig.class, TestFixtures.class})
class SubjectReportTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    TestFixtures fixtures;

    String user;
    Long member;
    List<Long> q;

    @BeforeEach
    void setUp() {
        fixtures.cleanAll();
        user = fixtures.userToken("user1");
        member = fixtures.memberId("user1");
        q = fixtures.seedExam("정보처리기사", 5).questionIds();
    }

    @Test
    void atRisk_whenBelowFailScore() throws Exception {
        record(q.get(0), true, "2026-11-01T10:00:00Z");
        for (int i = 1; i < 5; i++) {
            record(q.get(i), false, "2026-11-01T10:00:00Z");
        }

        report().andExpect(jsonPath("$[0].solvedCount").value(5))
                .andExpect(jsonPath("$[0].correctRate").value(20))
                .andExpect(jsonPath("$[0].atRisk").value(true))
                .andExpect(jsonPath("$[1].solvedCount").value(0))
                .andExpect(jsonPath("$[1].atRisk").value(false));
    }

    @Test
    void fewerThan5Solved_notAtRisk() throws Exception {
        for (int i = 0; i < 4; i++) {
            record(q.get(i), false, "2026-11-01T10:00:00Z");
        }
        report().andExpect(jsonPath("$[0].correctRate").value(0))
                .andExpect(jsonPath("$[0].atRisk").value(false));
    }

    @Test
    void usesLatestAttemptPerQuestion() throws Exception {
        for (int i = 0; i < 5; i++) {
            record(q.get(i), false, "2026-11-01T10:00:00Z");
            record(q.get(i), i < 4, "2026-11-02T10:00:00Z"); // 다시 풀어서 4개 맞힘
        }
        report().andExpect(jsonPath("$[0].solvedCount").value(5))
                .andExpect(jsonPath("$[0].correctRate").value(80))
                .andExpect(jsonPath("$[0].atRisk").value(false));
    }

    @Test
    void targetCertificate_winsOverLastSolved() throws Exception {
        Seed sqld = fixtures.seedExam("SQLD", 1);
        record(q.get(0), false, "2026-11-01T10:00:00Z"); // 마지막으로 푼 건 정보처리기사
        Long schedule = jdbc.queryForObject("""
                INSERT INTO exam_schedule (certificate_id, year, round, registration_start, registration_end, exam_date)
                VALUES (?, 2026, 1, '2026-01-01', '2026-01-05', '2026-02-01') RETURNING id""", Long.class,
                sqld.certificateId());
        jdbc.update("UPDATE member SET target_schedule_id = ? WHERE id = ?", schedule, member);

        // SQLD의 과목이 나온다: 정보처리기사 풀이는 SQLD 과목에 섞이지 않는다
        report().andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].solvedCount").value(0));
    }

    @Test
    void noTargetNoHistory_returnsEmpty() throws Exception {
        report().andExpect(jsonPath("$.length()").value(0));
    }

    void record(Long questionId, boolean correct, String solvedAt) {
        jdbc.update("""
                INSERT INTO solve_record (member_id, question_id, selected_no, correct, mode, solved_at)
                VALUES (?, ?, ?, ?, 'STUDY', ?::timestamptz)""", member, questionId, correct ? 1 : 2, correct,
                solvedAt);
    }

    ResultActions report() throws Exception {
        return mvc.perform(get("/api/me/subject-report").header("Authorization", "Bearer " + user))
                .andExpect(status().isOk());
    }
}
