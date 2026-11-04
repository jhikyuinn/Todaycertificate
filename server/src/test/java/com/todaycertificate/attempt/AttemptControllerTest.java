package com.todaycertificate.attempt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.todaycertificate.TestFixtures;
import com.todaycertificate.TestFixtures.Seed;
import com.todaycertificate.TestcontainersConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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

/** #13 시험 모드. seed: 과목 2개 × 2문제(1·2번 1과목, 3·4번 2과목), 정답 모두 1번, 합격 평균 60. */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfig.class, TestFixtures.class})
class AttemptControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    TestFixtures fixtures;

    String user;
    Seed seed;
    List<Long> q;

    @BeforeEach
    void setUp() {
        fixtures.cleanAll();
        user = fixtures.userToken("user1");
        seed = fixtures.seedExam("정보처리기사", 2);
        q = seed.questionIds();
    }

    // 1번 정답, 2번 오답, 3번 정답, 4번 안 풂 → 과목마다 50점, 평균 50.0 불합격
    String halfCorrect() {
        return answers(q.get(0), 1, q.get(1), 2, q.get(2), 1);
    }

    @Test
    void start_returnsQuestionsWithoutAnswerAndTimeLimit() throws Exception {
        mvc.perform(post("/api/exams/" + seed.examId() + "/attempts").header("Authorization", "Bearer " + user))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.attemptId").isNumber())
                .andExpect(jsonPath("$.timeLimitMinutes").value(60))
                .andExpect(jsonPath("$.questions.length()").value(4))
                .andExpect(jsonPath("$.questions[0].type").value("MULTIPLE_CHOICE"))
                .andExpect(jsonPath("$.questions[0]", not(hasKey("answerNo"))))
                .andExpect(jsonPath("$.questions[0]", not(hasKey("answerText"))));
    }

    @Test
    void submit_scoresAndSavesExamRecords() throws Exception {
        Long attempt = start(user);

        submit(user, attempt, halfCorrect())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageScore").value(50.0))
                .andExpect(jsonPath("$.passed").value(false))
                .andExpect(jsonPath("$.failScore").value(40))
                .andExpect(jsonPath("$.subjects[0].score").value(50))
                .andExpect(jsonPath("$.subjects[1].failed").value(false))
                .andExpect(jsonPath("$.questions[3].selectedNo").doesNotExist())
                .andExpect(jsonPath("$.questions[3].correct").value(false))
                .andExpect(jsonPath("$.questions[1].answerNo").value(1));

        // 안 푼 문제는 기록하지 않는다
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM solve_record WHERE mode = 'EXAM' AND attempt_id = ?", Integer.class, attempt))
                .isEqualTo(3);
        Map<String, Object> row = jdbc.queryForMap("SELECT * FROM exam_attempt WHERE id = ?", attempt);
        assertThat(row.get("submitted_at")).isNotNull();
        assertThat(row).containsEntry("average_score", 50.0).containsEntry("passed", false);
    }

    @Test
    void submit_allCorrect_passed() throws Exception {
        Long attempt = start(user);
        submit(user, attempt, answers(q.get(0), 1, q.get(1), 1, q.get(2), 1, q.get(3), 1))
                .andExpect(jsonPath("$.averageScore").value(100.0))
                .andExpect(jsonPath("$.passed").value(true));
    }

    @Test
    void submit_twice_returns409() throws Exception {
        Long attempt = start(user);
        submit(user, attempt, halfCorrect()).andExpect(status().isOk());
        submit(user, attempt, halfCorrect())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ALREADY_SUBMITTED"));
    }

    @Test
    void submit_concurrently_savesRecordsOnce() throws Exception {
        Long attempt = start(user);
        CountDownLatch ready = new CountDownLatch(1);
        Callable<Integer> task = () -> {
            ready.await();
            return submit(user, attempt, halfCorrect()).andReturn().getResponse().getStatus();
        };
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<Integer>> futures = new ArrayList<>(List.of(pool.submit(task), pool.submit(task)));
            ready.countDown(); // 두 스레드를 동시에 출발시킨다
            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> f : futures) {
                statuses.add(f.get());
            }
            assertThat(statuses).containsExactlyInAnyOrder(200, 409);
        } finally {
            pool.shutdown();
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM solve_record", Integer.class)).isEqualTo(3);
    }

    @Test
    void submit_otherMembersAttempt_returns403() throws Exception {
        Long attempt = start(user);
        submit(fixtures.userToken("other"), attempt, halfCorrect()).andExpect(status().isForbidden());
    }

    @Test
    void submit_questionFromOtherExam_returns400AndStaysOpen() throws Exception {
        Long attempt = start(user);
        Long foreign = fixtures.seedExam("SQLD", 1).questionIds().get(0);

        submit(user, attempt, answers(foreign, 1)).andExpect(status().isBadRequest());
        // 잘못된 요청은 응시를 닫지 않는다 → 다시 정상 제출 가능
        submit(user, attempt, halfCorrect()).andExpect(status().isOk());
    }

    @Test
    void submit_selectedNoOutOfRange_returns400() throws Exception {
        Long attempt = start(user);
        submit(user, attempt, answers(q.get(0), 0)).andExpect(status().isBadRequest());
    }

    @Test
    void getResult_afterSubmit_matchesSubmitResponse() throws Exception {
        Long attempt = start(user);
        String submitted = submit(user, attempt, halfCorrect()).andReturn().getResponse()
                .getContentAsString();

        String viewed = mvc.perform(get("/api/attempts/" + attempt).header("Authorization", "Bearer " + user))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(viewed).isEqualTo(submitted);
    }

    @Test
    void submit_mixedExam_gradesBoth() throws Exception {
        fixtures.cleanAll();
        user = fixtures.userToken("user1");
        seed = fixtures.seedExam("정보처리기사 실기", 1); // 1번 객관식, 2번 → 주관식
        q = seed.questionIds();
        fixtures.makeShortAnswer(q.get(1), "DDL|데이터 정의어");
        Long attempt = start(user);

        String json = "{\"answers\":[{\"questionId\":" + q.get(0) + ",\"selectedNo\":1},"
                + "{\"questionId\":" + q.get(1) + ",\"answerText\":\" 데이터 정의어 \"}]}";
        String submitted = submit(user, attempt, json)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageScore").value(100.0))
                .andExpect(jsonPath("$.questions[0].type").value("MULTIPLE_CHOICE"))
                .andExpect(jsonPath("$.questions[0].correct").value(true))
                .andExpect(jsonPath("$.questions[1].type").value("SHORT_ANSWER"))
                .andExpect(jsonPath("$.questions[1].correct").value(true))
                .andExpect(jsonPath("$.questions[1].answerText").value(" 데이터 정의어 "))
                .andExpect(jsonPath("$.questions[1].correctAnswerText").value("DDL|데이터 정의어"))
                .andExpect(jsonPath("$.questions[1].answerNo").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        String viewed = mvc.perform(get("/api/attempts/" + attempt).header("Authorization", "Bearer " + user))
                .andReturn().getResponse().getContentAsString();
        assertThat(viewed).isEqualTo(submitted);
    }

    @Test
    void submit_answerNotMatchingType_returns400AndStaysOpen() throws Exception {
        fixtures.makeShortAnswer(q.get(1), "DDL");
        Long attempt = start(user);

        submit(user, attempt, answers(q.get(1), 1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("selectedNo"));
        submit(user, attempt, halfCorrect().replace("\"selectedNo\":2", "\"answerText\":\"x\""))
                .andExpect(status().isOk());
    }

    @Test
    void getResult_beforeSubmit_returns404() throws Exception {
        Long attempt = start(user);
        mvc.perform(get("/api/attempts/" + attempt).header("Authorization", "Bearer " + user))
                .andExpect(status().isNotFound());
    }

    @Test
    void getResult_otherMember_returns403() throws Exception {
        Long attempt = start(user);
        submit(user, attempt, halfCorrect());
        mvc.perform(get("/api/attempts/" + attempt).header("Authorization", "Bearer " + fixtures.userToken("other")))
                .andExpect(status().isForbidden());
    }

    Long start(String token) throws Exception {
        String body = mvc.perform(post("/api/exams/" + seed.examId() + "/attempts")
                        .header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.attemptId")).longValue();
    }

    ResultActions submit(String token, Long attempt, String json) throws Exception {
        return mvc.perform(post("/api/attempts/" + attempt + "/submit")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    /** answers(문제ID, 선택, 문제ID, 선택, ...) → {"answers":[...]} */
    static String answers(Object... pairs) {
        List<String> items = new ArrayList<>();
        for (int i = 0; i < pairs.length; i += 2) {
            items.add("{\"questionId\":" + pairs[i] + ",\"selectedNo\":" + pairs[i + 1] + "}");
        }
        return "{\"answers\":[" + String.join(",", items) + "]}";
    }
}
