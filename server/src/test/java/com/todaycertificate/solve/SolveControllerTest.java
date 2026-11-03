package com.todaycertificate.solve;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.todaycertificate.TestFixtures;
import com.todaycertificate.TestFixtures.Seed;
import com.todaycertificate.TestcontainersConfig;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** #8 공부 모드 풀이. seed 문제의 정답은 모두 1번. */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfig.class, TestFixtures.class})
class SolveControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    TestFixtures fixtures;

    String user;
    Long questionId;

    @BeforeEach
    void setUp() {
        fixtures.cleanAll();
        user = fixtures.userToken("user1");
        Seed seed = fixtures.seedExam("정보처리기사", 1);
        questionId = seed.questionIds().get(0);
    }

    @Test
    void solve_correct_returnsTrueAndSavesRecord() throws Exception {
        solve(questionId, "{\"selectedNo\":1}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").value(true));

        Map<String, Object> row = jdbc.queryForMap("SELECT * FROM solve_record");
        assertThat(row).containsEntry("member_id", fixtures.memberId("user1"))
                .containsEntry("question_id", questionId).containsEntry("selected_no", 1)
                .containsEntry("correct", true).containsEntry("mode", "STUDY");
        assertThat(row.get("attempt_id")).isNull();
    }

    @Test
    void solve_wrong_returnsAnswerAndExplanation() throws Exception {
        solve(questionId, "{\"selectedNo\":3}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").value(false))
                .andExpect(jsonPath("$.answerNo").value(1))
                .andExpect(jsonPath("$.explanation").value("해설1"));
    }

    @Test
    void solve_selectedNoOutOfRange_returns400() throws Exception {
        solve(questionId, "{\"selectedNo\":5}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("selectedNo"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM solve_record", Integer.class)).isZero();
    }

    @Test
    void solve_unknownQuestion_returns404() throws Exception {
        solve(9999L, "{\"selectedNo\":1}").andExpect(status().isNotFound());
    }

    @Test
    void stats_nullBelow5() throws Exception {
        priorChoices(1, 1, 3); // 기존 3건 + 이번 1건 = 4건
        solve(questionId, "{\"selectedNo\":1}")
                .andExpect(jsonPath("$.stats").doesNotExist());
    }

    @Test
    void mostChosenWrong_ignoresCorrectChoice() throws Exception {
        priorChoices(1, 1, 1, 3, 3); // 정답 1번 3회, 3번 2회
        solve(questionId, "{\"selectedNo\":2}") // + 2번 1회 → 총 6회
                .andExpect(jsonPath("$.stats.solvedCount").value(6))
                .andExpect(jsonPath("$.stats.correctRate").value(50))
                .andExpect(jsonPath("$.stats.mostChosenWrongNo").value(3));
    }

    /** 다른 회원들이 이 문제에서 고른 보기라고 보고 기록을 미리 넣는다. */
    void priorChoices(int... selected) {
        Long member = fixtures.memberId("user1");
        for (int no : selected) {
            jdbc.update("""
                    INSERT INTO solve_record (member_id, question_id, selected_no, correct, mode, solved_at)
                    VALUES (?, ?, ?, ?, 'STUDY', now())""", member, questionId, no, no == 1);
        }
    }

    ResultActions solve(Long id, String json) throws Exception {
        return mvc.perform(post("/api/questions/" + id + "/solve")
                .header("Authorization", "Bearer " + user)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }
}
