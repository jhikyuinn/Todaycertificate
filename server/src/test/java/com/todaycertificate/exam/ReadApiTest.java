package com.todaycertificate.exam;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasKey;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.todaycertificate.TestFixtures;
import com.todaycertificate.TestFixtures.Seed;
import com.todaycertificate.TestcontainersConfig;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** #7 자격증 목록·상세, 회차 문제 조회. */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfig.class, TestFixtures.class})
class ReadApiTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    TestFixtures fixtures;

    String user;
    Seed seed;

    @BeforeEach
    void setUp() {
        fixtures.cleanAll();
        user = fixtures.userToken("user1");
        seed = fixtures.seedExam("정보처리기사", 2);
    }

    @Test
    void list_returnsCertificates() throws Exception {
        get(user, "/api/certificates")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("정보처리기사"));
    }

    @Test
    void detail_includesOnlyCurrentYearSchedules() throws Exception {
        int thisYear = LocalDate.now(ZoneId.of("Asia/Seoul")).getYear();
        insertSchedule(thisYear, 1);
        insertSchedule(thisYear - 1, 3);
        jdbc.update("INSERT INTO exam (certificate_id, year, round) VALUES (?, 2026, 2)", seed.certificateId());

        get(user, "/api/certificates/" + seed.certificateId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subjects.length()").value(2))
                .andExpect(jsonPath("$.schedules.length()").value(1))
                .andExpect(jsonPath("$.schedules[0].year").value(thisYear))
                // 최신 회차부터, 회차별 문제 수 (문제 없는 회차는 0)
                .andExpect(jsonPath("$.exams[0].year").value(2026))
                .andExpect(jsonPath("$.exams[0].questionCount").value(0))
                .andExpect(jsonPath("$.exams[1].questionCount").value(4));
    }

    @Test
    void detail_unknownId_returns404() throws Exception {
        get(user, "/api/certificates/9999")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void questions_doNotExposeAnswer() throws Exception {
        get(user, "/api/exams/" + seed.examId() + "/questions")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]", not(hasKey("answerNo"))))
                .andExpect(jsonPath("$[0]", not(hasKey("explanation"))))
                .andExpect(jsonPath("$[0].choices.length()").value(4));
    }

    @Test
    void questions_orderedByNumber() throws Exception {
        get(user, "/api/exams/" + seed.examId() + "/questions")
                .andExpect(jsonPath("$[*].number", contains(1, 2, 3, 4)))
                .andExpect(jsonPath("$[2].subjectNumber").value(2));
    }

    @Test
    void questions_bookmarkedFlag_onlyMine() throws Exception {
        fixtures.userToken("other");
        jdbc.update("INSERT INTO bookmark (member_id, question_id) VALUES (?, ?), (?, ?)",
                fixtures.memberId("user1"), seed.questionIds().get(0),
                fixtures.memberId("other"), seed.questionIds().get(1));

        get(user, "/api/exams/" + seed.examId() + "/questions")
                .andExpect(jsonPath("$[*].bookmarked", contains(true, false, false, false)));
    }

    @Test
    void questions_withoutToken_returns401() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/exams/" + seed.examId() + "/questions"))
                .andExpect(status().isUnauthorized());
    }

    void insertSchedule(int year, int round) {
        LocalDate d = LocalDate.of(year, 3, 1);
        jdbc.update("""
                INSERT INTO exam_schedule (certificate_id, year, round, registration_start, registration_end, exam_date)
                VALUES (?, ?, ?, ?, ?, ?)""", seed.certificateId(), year, round, d, d.plusDays(3), d.plusDays(30));
    }

    ResultActions get(String token, String url) throws Exception {
        return mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(url)
                .header("Authorization", "Bearer " + token));
    }
}
