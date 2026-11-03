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

    ResultActions solve(Long id, String json) throws Exception {
        return mvc.perform(post("/api/questions/" + id + "/solve")
                .header("Authorization", "Bearer " + user)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }
}
