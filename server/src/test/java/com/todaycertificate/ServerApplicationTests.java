package com.todaycertificate;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
@Import(TestcontainersConfig.class)
class ServerApplicationTests {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void contextLoads() {
    }

    @Test
    void sampleData_isLoaded() {
        assertThat(jdbc.queryForObject("SELECT count(*) FROM question", Integer.class)).isEqualTo(14);
    }

    @Test
    void codeQuestion_keepsNewlinesAndIndentation() {
        String content = jdbc.queryForObject(
                "SELECT content FROM question WHERE content LIKE '%GROUP BY%'", String.class);
        assertThat(content).contains("```\nSELECT DEPT_NO, COUNT(*)\nFROM EMP\nGROUP BY DEPT_NO;\n```");

        String java = jdbc.queryForObject(
                "SELECT content FROM question WHERE content LIKE '%continue%'", String.class);
        assertThat(java).contains("\n    if (i % 2 == 0) continue;\n");
    }
}
