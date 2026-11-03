package com.todaycertificate.study;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.todaycertificate.TestFixtures;
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

/** #20 오늘의 문제. seed: 과목마다 15문제 = 30문제 (q0~q14 1과목). */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfig.class, TestFixtures.class})
class DailyProblemTest {

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
        q = fixtures.seedExam("정보처리기사", 15).questionIds();
    }

    @Test
    void noTarget_noHistory_returnsEmpty() throws Exception {
        assertThat(daily()).isEmpty();
    }

    @Test
    void neverMoreThan20() throws Exception {
        record(q.get(0), true, "2 days");
        List<Long> ids = daily();
        assertThat(ids).hasSize(20).doesNotHaveDuplicates();
    }

    @Test
    void wrongNotesComeFirst_max8() throws Exception {
        List<Long> wrong = q.subList(0, 10);
        wrong.forEach(id -> record(id, false, "2 days"));

        List<Long> ids = daily();
        assertThat(ids.subList(0, 8)).allMatch(wrong::contains);
        assertThat(ids.stream().filter(wrong::contains)).hasSize(8); // 나머지는 안 푼 문제로 채움
    }

    @Test
    void wrongSolvedToday_isSkipped() throws Exception {
        record(q.get(0), false, "0 days");
        record(q.get(1), false, "2 days");
        assertThat(daily().get(0)).isEqualTo(q.get(1));
    }

    @Test
    void sameDay_returnsSameSet() throws Exception {
        record(q.get(0), false, "2 days");
        assertThat(daily()).isEqualTo(daily());
    }

    void record(Long questionId, boolean correct, String ago) {
        jdbc.update("""
                INSERT INTO solve_record (member_id, question_id, selected_no, correct, mode, solved_at)
                VALUES (?, ?, ?, ?, 'STUDY', now() - ?::interval)""", member, questionId, correct ? 1 : 2, correct,
                ago);
    }

    List<Long> daily() throws Exception {
        String body = mvc.perform(get("/api/me/daily-problems").header("Authorization", "Bearer " + user))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        List<Number> ids = JsonPath.read(body, "$[*].id");
        return ids.stream().map(Number::longValue).toList();
    }
}
