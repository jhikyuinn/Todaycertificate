package com.todaycertificate;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

// 로컬과 운영에 들어가는 샘플 데이터(db/sample)가 깨지지 않았는지 확인한다.
// 다른 테스트는 샘플 없이 돌기 때문에 여기서만 샘플 위치를 다시 넣는다.
@SpringBootTest(properties = "spring.flyway.locations=classpath:db/migration,classpath:db/sample")
@Import(TestcontainersConfig.class)
class SampleDataTest {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void sampleData_isLoaded() {
        assertThat(jdbc.queryForObject("SELECT count(*) FROM question", Integer.class)).isEqualTo(50);
    }

    @Test
    void codeQuestion_keepsNewlinesAndIndentation() {
        String content = jdbc.queryForObject(
                "SELECT content FROM question WHERE content LIKE '%GROUP BY DEPT_NO%'", String.class);
        assertThat(content).contains("```\nSELECT DEPT_NO, COUNT(*)\nFROM EMP\nGROUP BY DEPT_NO;\n```");

        String java = jdbc.queryForObject(
                "SELECT content FROM question WHERE content LIKE '%continue%'", String.class);
        assertThat(java).contains("\n    if (i % 2 == 0) continue;\n");
    }
}
