package com.todaycertificate.member;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.todaycertificate.TestFixtures;
import com.todaycertificate.TestcontainersConfig;
import java.time.LocalDate;
import java.time.ZoneId;
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

/** #21 D-day 목표. */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfig.class, TestFixtures.class})
class MeControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    TestFixtures fixtures;

    String user;
    Long certId;
    LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));

    @BeforeEach
    void setUp() {
        fixtures.cleanAll();
        user = fixtures.userToken("user1");
        certId = fixtures.seedExam("정보처리기사", 1).certificateId();
    }

    @Test
    void dDay_isZeroOnExamDay() throws Exception {
        setTarget(schedule(today)).andExpect(status().isNoContent());

        me().andExpect(jsonPath("$.target.certificateName").value("정보처리기사"))
                .andExpect(jsonPath("$.target.dDay").value(0));
    }

    @Test
    void dDay_countsDaysUntilExam() throws Exception {
        setTarget(schedule(today.plusDays(23)));
        me().andExpect(jsonPath("$.target.dDay").value(23));
    }

    @Test
    void target_canBeCleared() throws Exception {
        setTarget(schedule(today.plusDays(10)));
        mvc.perform(put("/api/me/target").header("Authorization", "Bearer " + user)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"scheduleId\":null}"))
                .andExpect(status().isNoContent());

        me().andExpect(jsonPath("$.target").doesNotExist());
    }

    @Test
    void target_unknownSchedule_returns404() throws Exception {
        setTarget(9999L).andExpect(status().isNotFound());
    }

    Long schedule(LocalDate examDate) {
        return jdbc.queryForObject("""
                INSERT INTO exam_schedule (certificate_id, year, round, registration_start, registration_end, exam_date)
                VALUES (?, ?, 1, ?, ?, ?) RETURNING id""", Long.class, certId, examDate.getYear(),
                examDate.minusDays(40), examDate.minusDays(35), examDate);
    }

    ResultActions setTarget(Long scheduleId) throws Exception {
        return mvc.perform(put("/api/me/target").header("Authorization", "Bearer " + user)
                .contentType(MediaType.APPLICATION_JSON).content("{\"scheduleId\":" + scheduleId + "}"));
    }

    ResultActions me() throws Exception {
        return mvc.perform(get("/api/me").header("Authorization", "Bearer " + user)).andExpect(status().isOk());
    }
}
