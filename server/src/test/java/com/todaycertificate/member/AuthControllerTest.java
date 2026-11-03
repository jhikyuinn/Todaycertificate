package com.todaycertificate.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.todaycertificate.TestcontainersConfig;
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
@Import(TestcontainersConfig.class)
class AuthControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void cleanMembers() {
        jdbc.update("DELETE FROM member");
    }

    @Test
    void signup_thenLogin_returnsToken() throws Exception {
        signup("tester1", "password123").andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber());

        login("tester1", "password123").andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.member.loginId").value("tester1"))
                .andExpect(jsonPath("$.member.role").value("USER"));
    }

    @Test
    void signup_duplicateLoginId_returns409() throws Exception {
        signup("tester1", "password123").andExpect(status().isCreated());

        signup("tester1", "password456").andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_LOGIN_ID"));
    }

    @Test
    void signup_invalidInput_returns400() throws Exception {
        signup("ab", "short").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void login_wrongPassword_returns401() throws Exception {
        signup("tester1", "password123");

        login("tester1", "wrong-password").andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        // 없는 아이디도 같은 오류: 어떤 아이디가 가입돼 있는지 알려주지 않는다
        login("nobody", "password123").andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void password_isStoredAsBcryptHash() throws Exception {
        signup("tester1", "password123");

        String stored = jdbc.queryForObject("SELECT password FROM member WHERE login_id = 'tester1'", String.class);
        assertThat(stored).startsWith("$2").isNotEqualTo("password123");
    }

    @Test
    void me_withToken_returnsMyInfo() throws Exception {
        signup("tester1", "password123");
        String token = tokenOf("tester1", "password123");

        mvc.perform(get("/api/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loginId").value("tester1"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void me_withoutToken_returns401Json() throws Exception {
        mvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void me_withGarbageToken_returns401Json() throws Exception {
        mvc.perform(get("/api/me").header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void adminApi_withUserToken_returns403() throws Exception {
        signup("tester1", "password123");
        String token = tokenOf("tester1", "password123");

        mvc.perform(get("/api/admin/certificates").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    private ResultActions signup(String loginId, String password) throws Exception {
        String body = """
                {"loginId": "%s", "name": "테스터", "password": "%s"}""".formatted(loginId, password);
        return mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions login(String loginId, String password) throws Exception {
        String body = """
                {"loginId": "%s", "password": "%s"}""".formatted(loginId, password);
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private String tokenOf(String loginId, String password) throws Exception {
        String json = login(loginId, password).andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.accessToken");
    }
}
