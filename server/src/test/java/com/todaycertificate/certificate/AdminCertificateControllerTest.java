package com.todaycertificate.certificate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.todaycertificate.TestFixtures;
import com.todaycertificate.TestcontainersConfig;
import java.util.List;
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

@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfig.class, TestFixtures.class})
class AdminCertificateControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    TestFixtures fixtures;

    String admin;

    @BeforeEach
    void setUp() {
        fixtures.cleanAll();
        admin = fixtures.adminToken("admin1");
    }

    @Test
    void create_withSubjects_persistsAll() throws Exception {
        Long id = createCertificate("정보처리기사", "1:소프트웨어 설계", "2:소프트웨어 개발");

        Map<String, Object> row = jdbc.queryForMap("SELECT * FROM certificate WHERE id = ?", id);
        assertThat(row).containsEntry("name", "정보처리기사").containsEntry("time_limit_minutes", 150)
                .containsEntry("fail_score", 40).containsEntry("pass_average", 60);
        assertThat(subjects(id)).containsExactly("1:소프트웨어 설계", "2:소프트웨어 개발");
    }

    @Test
    void update_renamesAddsAndRemovesSubjects() throws Exception {
        Long id = createCertificate("정보처리기사", "1:소프트웨어 설계", "2:소프트웨어 개발");

        send(put("/api/admin/certificates/" + id), certificateJson("정보처리기사 필기", "1:요구사항 확인", "3:데이터베이스 구축"))
                .andExpect(status().isNoContent());

        assertThat(jdbc.queryForObject("SELECT name FROM certificate WHERE id = ?", String.class, id))
                .isEqualTo("정보처리기사 필기");
        assertThat(subjects(id)).containsExactly("1:요구사항 확인", "3:데이터베이스 구축");
    }

    @Test
    void update_removingSubjectWithQuestions_returns409AndChangesNothing() throws Exception {
        Long id = createCertificate("정보처리기사", "1:소프트웨어 설계", "2:소프트웨어 개발");
        addQuestionToSubject(id, 2);

        // 1과목 이름 변경 + 문제가 있는 2과목 삭제 → 전체가 반영되지 않아야 한다
        send(put("/api/admin/certificates/" + id), certificateJson("바뀐 이름", "1:바뀐 과목"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SUBJECT_IN_USE"));

        assertThat(jdbc.queryForObject("SELECT name FROM certificate WHERE id = ?", String.class, id))
                .isEqualTo("정보처리기사");
        assertThat(subjects(id)).containsExactly("1:소프트웨어 설계", "2:소프트웨어 개발");
    }

    @Test
    void create_duplicateName_returns409() throws Exception {
        createCertificate("SQLD", "1:데이터 모델링의 이해");

        send(post("/api/admin/certificates"), certificateJson("SQLD", "1:데이터 모델링의 이해"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_NAME"));
    }

    @Test
    void create_duplicateSubjectNumber_returns400() throws Exception {
        send(post("/api/admin/certificates"), certificateJson("SQLD", "1:모델링", "1:SQL"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void addSchedule_thenDelete() throws Exception {
        Long id = createCertificate("SQLD", "1:데이터 모델링의 이해");

        String json = send(post("/api/admin/certificates/" + id + "/schedules"), scheduleJson("2026-11-09", "2026-11-13", "2026-12-05"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long scheduleId = ((Number) JsonPath.read(json, "$.id")).longValue();
        assertThat(countSchedules(id)).isEqualTo(1);

        send(delete("/api/admin/schedules/" + scheduleId), "").andExpect(status().isNoContent());
        assertThat(countSchedules(id)).isZero();
    }

    @Test
    void addSchedule_registrationAfterExamDate_returns400() throws Exception {
        Long id = createCertificate("SQLD", "1:데이터 모델링의 이해");

        send(post("/api/admin/certificates/" + id + "/schedules"), scheduleJson("2026-12-10", "2026-12-20", "2026-12-05"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void deleteSchedule_clearsMembersTarget() throws Exception {
        Long id = createCertificate("SQLD", "1:데이터 모델링의 이해");
        String json = send(post("/api/admin/certificates/" + id + "/schedules"), scheduleJson("2026-11-09", "2026-11-13", "2026-12-05"))
                .andReturn().getResponse().getContentAsString();
        long scheduleId = ((Number) JsonPath.read(json, "$.id")).longValue();
        fixtures.userToken("user1");
        jdbc.update("UPDATE member SET target_schedule_id = ? WHERE login_id = 'user1'", scheduleId);

        send(delete("/api/admin/schedules/" + scheduleId), "").andExpect(status().isNoContent());

        assertThat(jdbc.queryForObject("SELECT target_schedule_id FROM member WHERE login_id = 'user1'", Long.class))
                .isNull();
    }

    @Test
    void unknownCertificate_returns404() throws Exception {
        send(put("/api/admin/certificates/999"), certificateJson("없음", "1:과목"))
                .andExpect(status().isNotFound());
    }

    // --- helpers ---

    private ResultActions send(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,
                               String body) throws Exception {
        return mvc.perform(request.header("Authorization", "Bearer " + admin)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private Long createCertificate(String name, String... subjects) throws Exception {
        String json = send(post("/api/admin/certificates"), certificateJson(name, subjects))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }

    /** subjects: "번호:이름" 형식 */
    private static String certificateJson(String name, String... subjects) {
        String subjectJson = String.join(",", List.of(subjects).stream()
                .map(s -> s.split(":", 2))
                .map(p -> "{\"number\": %s, \"name\": \"%s\"}".formatted(p[0], p[1]))
                .toList());
        return """
                {"name": "%s", "description": "설명", "timeLimitMinutes": 150, "failScore": 40,
                 "passAverage": 60, "subjects": [%s]}""".formatted(name, subjectJson);
    }

    private static String scheduleJson(String regStart, String regEnd, String examDate) {
        return """
                {"year": 2026, "round": 4, "registrationStart": "%s", "registrationEnd": "%s", "examDate": "%s"}"""
                .formatted(regStart, regEnd, examDate);
    }

    private List<String> subjects(Long certificateId) {
        return jdbc.queryForList("SELECT number || ':' || name FROM subject WHERE certificate_id = ? ORDER BY number",
                String.class, certificateId);
    }

    private int countSchedules(Long certificateId) {
        return jdbc.queryForObject("SELECT count(*) FROM exam_schedule WHERE certificate_id = ?", Integer.class,
                certificateId);
    }

    private void addQuestionToSubject(Long certificateId, int subjectNumber) {
        jdbc.update("INSERT INTO exam (certificate_id, year, round) VALUES (?, 2025, 1)", certificateId);
        jdbc.update("""
                INSERT INTO question (exam_id, subject_id, number, content, choice1, choice2, choice3, choice4, answer_no)
                SELECT e.id, s.id, 1, '문제', '1', '2', '3', '4', 1
                FROM exam e JOIN subject s ON s.certificate_id = e.certificate_id AND s.number = ?
                WHERE e.certificate_id = ?""", subjectNumber, certificateId);
    }
}
