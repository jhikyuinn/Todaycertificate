package com.todaycertificate.solve;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.test.web.servlet.ResultActions;

/** #15 별표·오답노트. seed 정답은 모두 1번. */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfig.class, TestFixtures.class})
class NoteControllerTest {

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
        q = fixtures.seedExam("정보처리기사", 2).questionIds();
    }

    @Test
    void wrongThenCorrect_removedFromNotes() throws Exception {
        record(member, q.get(0), false, "STUDY", "2026-11-01T10:00:00Z");
        record(member, q.get(0), true, "STUDY", "2026-11-02T10:00:00Z"); // 다시 맞힘
        record(member, q.get(1), true, "STUDY", "2026-11-01T10:00:00Z");
        record(member, q.get(1), false, "STUDY", "2026-11-02T10:00:00Z"); // 맞혔다가 틀림

        call(get("/api/me/wrong-notes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].question.id", contains(q.get(1).intValue())))
                .andExpect(jsonPath("$[0].certificateName").value("정보처리기사"))
                .andExpect(jsonPath("$[0].examYear").value(2025))
                .andExpect(jsonPath("$[0].examRound").value(1));
    }

    @Test
    void wrongInExamMode_appearsInNotes() throws Exception {
        record(member, q.get(2), false, "EXAM", "2026-11-01T10:00:00Z");

        call(get("/api/me/wrong-notes"))
                .andExpect(jsonPath("$[*].question.id", contains(q.get(2).intValue())));
    }

    @Test
    void bookmark_isIdempotent() throws Exception {
        call(put("/api/questions/" + q.get(0) + "/bookmark")).andExpect(status().isNoContent());
        call(put("/api/questions/" + q.get(0) + "/bookmark")).andExpect(status().isNoContent());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM bookmark", Integer.class)).isEqualTo(1);

        call(get("/api/me/bookmarks"))
                .andExpect(jsonPath("$[*].question.id", contains(q.get(0).intValue())))
                .andExpect(jsonPath("$[0].question.bookmarked").value(true));

        call(delete("/api/questions/" + q.get(0) + "/bookmark")).andExpect(status().isNoContent());
        call(delete("/api/questions/" + q.get(0) + "/bookmark")).andExpect(status().isNoContent());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM bookmark", Integer.class)).isZero();
    }

    @Test
    void bookmark_unknownQuestion_returns404() throws Exception {
        call(put("/api/questions/9999/bookmark")).andExpect(status().isNotFound());
    }

    @Test
    void bookmarks_newestFirst() throws Exception {
        call(put("/api/questions/" + q.get(0) + "/bookmark"));
        call(put("/api/questions/" + q.get(3) + "/bookmark"));

        call(get("/api/me/bookmarks"))
                .andExpect(jsonPath("$[*].question.id", contains(q.get(3).intValue(), q.get(0).intValue())));
    }

    @Test
    void notes_onlyMine() throws Exception {
        fixtures.userToken("other");
        Long other = fixtures.memberId("other");
        record(other, q.get(0), false, "STUDY", "2026-11-01T10:00:00Z");
        jdbc.update("INSERT INTO bookmark (member_id, question_id) VALUES (?, ?)", other, q.get(0));

        call(get("/api/me/wrong-notes")).andExpect(jsonPath("$.length()").value(0));
        call(get("/api/me/bookmarks")).andExpect(jsonPath("$.length()").value(0));
    }

    void record(Long memberId, Long questionId, boolean correct, String mode, String solvedAt) {
        jdbc.update("""
                INSERT INTO solve_record (member_id, question_id, selected_no, correct, mode, solved_at)
                VALUES (?, ?, ?, ?, ?, ?::timestamptz)""", memberId, questionId, correct ? 1 : 2, correct, mode,
                solvedAt);
    }

    ResultActions call(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request)
            throws Exception {
        return mvc.perform(request.header("Authorization", "Bearer " + user));
    }
}
