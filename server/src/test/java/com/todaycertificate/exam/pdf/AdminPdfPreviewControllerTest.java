package com.todaycertificate.exam.pdf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
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
class AdminPdfPreviewControllerTest {

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
        seed = fixtures.seedExam("정보처리기사", 2); // 1·2과목, 문제 4개
    }

    @Test
    void preview_samplePdf_returnsRowsAndSavesNothing() throws Exception {
        int before = questionCount();

        preview(admin, seed.certificateId(), samplePdf())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows.length()").value(10))
                .andExpect(jsonPath("$.rows[0].number").value(1))
                .andExpect(jsonPath("$.rows[0].choices.length()").value(4))
                .andExpect(jsonPath("$.rows[*].warnings[*]").isEmpty());

        assertThat(questionCount()).isEqualTo(before);
    }

    @Test
    void preview_notPdf_returns400PdfInvalid() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "q.pdf", "application/pdf",
                "이건 PDF가 아니다".getBytes(StandardCharsets.UTF_8));
        preview(admin, seed.certificateId(), file)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PDF_INVALID"));
    }

    @Test
    void preview_asUser_returns403() throws Exception {
        preview(fixtures.userToken("user1"), seed.certificateId(), samplePdf())
                .andExpect(status().isForbidden());
    }

    @Test
    void preview_unknownCertificate_returns404() throws Exception {
        preview(admin, 9999L, samplePdf()).andExpect(status().isNotFound());
    }

    @Test
    void preview_unknownSubject_addsWarning() throws Exception {
        jdbc.update("DELETE FROM question WHERE subject_id IN (SELECT id FROM subject WHERE number = 2)");
        jdbc.update("DELETE FROM subject WHERE number = 2");

        preview(admin, seed.certificateId(), samplePdf())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows[0].warnings").isEmpty())
                .andExpect(jsonPath("$.rows[9].warnings", hasItem("등록되지 않은 과목번호입니다: 2")));
    }

    int questionCount() {
        return jdbc.queryForObject("SELECT count(*) FROM question", Integer.class);
    }

    MockMultipartFile samplePdf() throws Exception {
        try (var in = getClass().getResourceAsStream("/pdf/sample-exam.pdf")) {
            return new MockMultipartFile("file", "sample-exam.pdf", "application/pdf", in.readAllBytes());
        }
    }

    ResultActions preview(String token, Long certificateId, MockMultipartFile file) throws Exception {
        return mvc.perform(multipart("/api/admin/certificates/" + certificateId + "/exams/pdf-preview")
                .file(file).header("Authorization", "Bearer " + token));
    }
}
