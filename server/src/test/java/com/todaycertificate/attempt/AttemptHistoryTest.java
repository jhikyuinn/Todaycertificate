package com.todaycertificate.attempt;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.todaycertificate.TestFixtures;
import com.todaycertificate.TestFixtures.Seed;
import com.todaycertificate.TestcontainersConfig;
import java.sql.Timestamp;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** #32 시험 점수 이력: GET /api/me/attempts?certificateId= */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfig.class, TestFixtures.class})
class AttemptHistoryTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    TestFixtures fixtures;

    String user;
    Long me;
    Seed seed;

    @BeforeEach
    void setUp() {
        fixtures.cleanAll();
        user = fixtures.userToken("user1");
        me = fixtures.memberId("user1");
        seed = fixtures.seedExam("정보처리기사", 1);
    }

    @Test
    void history_onlySubmittedAndMine_oldestFirst() throws Exception {
        Long olderExam = jdbc.queryForObject(
                "INSERT INTO exam (certificate_id, year, round) VALUES (?, 2024, 2) RETURNING id", Long.class,
                seed.certificateId());
        fixtures.userToken("other");
        Long other = fixtures.memberId("other");

        // 넣는 순서를 제출 순서와 다르게 해서, id 순이 아니라 제출 시각 순으로 정렬되는지 본다
        Long later = attempt(me, seed.examId(), "2026-11-02T10:00:00Z", 75.0, true);
        Long earlier = attempt(me, olderExam, "2026-11-01T10:00:00Z", 50.0, false);
        attempt(me, seed.examId(), null, null, null); // 시작만 하고 제출 안 함
        attempt(other, seed.examId(), "2026-11-01T12:00:00Z", 90.0, true);

        history(seed.certificateId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].attemptId").value(earlier))
                .andExpect(jsonPath("$[0].examYear").value(2024))
                .andExpect(jsonPath("$[0].examRound").value(2))
                .andExpect(jsonPath("$[0].averageScore").value(50.0))
                .andExpect(jsonPath("$[0].passed").value(false))
                .andExpect(jsonPath("$[0].submittedAt").value("2026-11-01T10:00:00Z"))
                .andExpect(jsonPath("$[1].attemptId").value(later))
                .andExpect(jsonPath("$[1].passed").value(true));
    }

    @Test
    void history_otherCertificateExcluded() throws Exception {
        Seed sqld = fixtures.seedExam("SQLD", 1);
        attempt(me, sqld.examId(), "2026-11-01T10:00:00Z", 80.0, true);

        history(seed.certificateId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void history_unknownCertificate_returns404() throws Exception {
        history(999_999L).andExpect(status().isNotFound());
    }

    ResultActions history(Long certificateId) throws Exception {
        return mvc.perform(get("/api/me/attempts").param("certificateId", String.valueOf(certificateId))
                .header("Authorization", "Bearer " + user));
    }

    Long attempt(Long memberId, Long examId, String submittedAt, Double score, Boolean passed) {
        return jdbc.queryForObject("""
                INSERT INTO exam_attempt (member_id, exam_id, started_at, submitted_at, average_score, passed)
                VALUES (?, ?, now(), ?, ?, ?) RETURNING id""", Long.class,
                memberId, examId, submittedAt == null ? null : Timestamp.from(Instant.parse(submittedAt)), score, passed);
    }
}
