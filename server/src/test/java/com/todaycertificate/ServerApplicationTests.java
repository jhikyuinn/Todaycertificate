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
    void sampleData_isNotLoadedInTests() {
        assertThat(jdbc.queryForObject("SELECT count(*) FROM question", Integer.class)).isZero();
    }
}
