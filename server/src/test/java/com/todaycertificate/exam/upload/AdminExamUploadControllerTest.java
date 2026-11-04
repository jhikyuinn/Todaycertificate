package com.todaycertificate.exam.upload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.todaycertificate.TestFixtures;
import com.todaycertificate.TestFixtures.Seed;
import com.todaycertificate.TestcontainersConfig;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfig.class, TestFixtures.class})
class AdminExamUploadControllerTest {

    static final String HEADER = "과목번호,문제번호,문제,보기1,보기2,보기3,보기4,정답번호,해설\n";

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    TestFixtures fixtures;

    String admin;
    Seed seed;

    @BeforeEach
    void setUp() {
        fixtures.cleanAll();
        admin = fixtures.adminToken("admin1");
        seed = fixtures.seedExam("정보처리기사", 2); // 2025년 1회, 문제 1~4번
    }

    @Test
    void firstUpload_createsExamAndQuestions() throws Exception {
        upload(admin, 2024, 2, HEADER + "1,1,새 문제1,a,b,c,d,2,\n2,2,새 문제2,a,b,c,d,3,해설\n")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created").value(2))
                .andExpect(jsonPath("$.updated").value(0))
                .andExpect(jsonPath("$.deleted").value(0));

        assertThat(jdbc.queryForList("""
                SELECT q.content FROM question q JOIN exam e ON e.id = q.exam_id
                WHERE e.year = 2024 AND e.round = 2 ORDER BY q.number""", String.class))
                .containsExactly("새 문제1", "새 문제2");
    }

    @Test
    void reupload_updatesByNumberAndKeepsSolveRecords() throws Exception {
        fixtures.userToken("user1");
        Long member = fixtures.memberId("user1");
        Long q1 = seed.questionIds().get(0);
        Long q4 = seed.questionIds().get(3);
        jdbc.update("""
                INSERT INTO solve_record (member_id, question_id, selected_no, correct, mode, solved_at)
                VALUES (?, ?, 2, false, 'STUDY', now()), (?, ?, 1, true, 'STUDY', now())""", member, q1, member, q4);
        jdbc.update("INSERT INTO bookmark (member_id, question_id) VALUES (?, ?)", member, q4);

        // 1번은 내용 수정, 2·3번은 그대로, 4번은 파일에서 빠짐
        upload(admin, 2025, 1, HEADER + "1,1,고친 문제1,a,b,c,d,1,\n1,2,문제2,a,b,c,d,1,\n2,3,문제3,a,b,c,d,1,\n")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.examId").value(seed.examId()))
                .andExpect(jsonPath("$.created").value(0))
                .andExpect(jsonPath("$.updated").value(3))
                .andExpect(jsonPath("$.deleted").value(1));

        assertThat(jdbc.queryForObject("SELECT content FROM question WHERE id = ?", String.class, q1))
                .isEqualTo("고친 문제1");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM solve_record WHERE question_id = ?", Integer.class, q1))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM question WHERE id = ?", Integer.class, q4)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM bookmark", Integer.class)).isZero();
    }

    @Test
    void reupload_changesTypeAndKeepsSolveRecords() throws Exception {
        fixtures.userToken("user1");
        Long member = fixtures.memberId("user1");
        Long q1 = seed.questionIds().get(0);
        jdbc.update("""
                INSERT INTO solve_record (member_id, question_id, selected_no, correct, mode, solved_at)
                VALUES (?, ?, 2, false, 'STUDY', now())""", member, q1);

        // 1번을 객관식 → 주관식으로 바꾼다. 보기·정답번호가 비워져야 DB CHECK를 통과한다
        upload(admin, 2025, 1, "과목번호,문제번호,문제,보기1,보기2,보기3,보기4,정답번호,해설,유형\n"
                + "1,1,정의어는?,,,,,DDL|데이터 정의어,,주관식-정확히\n")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updated").value(1));

        assertThat(jdbc.queryForMap("SELECT type, answer_text, exact_match, choice1, answer_no FROM question WHERE id = ?", q1))
                .containsEntry("type", "SHORT_ANSWER")
                .containsEntry("answer_text", "DDL|데이터 정의어")
                .containsEntry("exact_match", true)
                .containsEntry("choice1", null)
                .containsEntry("answer_no", null);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM solve_record WHERE question_id = ?", Integer.class, q1))
                .isEqualTo(1);
    }

    @Test
    void invalidFile_savesNothing() throws Exception {
        // 2번째 행 정답번호 오류, 3번째 행 없는 과목번호
        upload(admin, 2024, 2, HEADER + "1,1,문제,a,b,c,d,1,\n1,2,문제,a,b,c,d,5,\n9,3,문제,a,b,c,d,1,\n")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CSV_INVALID"))
                .andExpect(jsonPath("$.details.length()").value(2))
                .andExpect(jsonPath("$.details[0].row").value(2));

        assertThat(jdbc.queryForObject("SELECT count(*) FROM exam WHERE year = 2024", Integer.class)).isZero();
    }

    @Test
    void upload_withUserToken_returns403() throws Exception {
        upload(fixtures.userToken("user1"), 2024, 2, HEADER + "1,1,문제,a,b,c,d,1,\n")
                .andExpect(status().isForbidden());
    }

    @Test
    void upload_withoutFile_returns400() throws Exception {
        mvc.perform(multipart("/api/admin/certificates/" + seed.certificateId() + "/exams")
                        .param("year", "2024").param("round", "2")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    ResultActions upload(String token, int year, int round, String csv) throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "q.csv", "text/csv",
                csv.getBytes(StandardCharsets.UTF_8));
        return mvc.perform(multipart("/api/admin/certificates/" + seed.certificateId() + "/exams")
                .file(file).param("year", String.valueOf(year)).param("round", String.valueOf(round))
                .header("Authorization", "Bearer " + token));
    }
}
