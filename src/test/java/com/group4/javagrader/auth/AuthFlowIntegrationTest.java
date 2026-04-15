package com.group4.javagrader.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@ActiveProfiles("test")
class AuthFlowIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private MockMvc mockMvc;

    @BeforeEach
    void setUpUser() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        jdbcTemplate.update("delete from users");
        jdbcTemplate.update(
                """
                insert into users (username, password_hash, full_name, role, enabled)
                values (?, ?, ?, ?, ?)
                """,
                "duc.teacher",
                passwordEncoder.encode("Password123!"),
                "Nguyen Minh Duc",
                "TEACHER",
                true
        );
    }

    @Test
    void getLoginPageShouldRenderCustomLoginView() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"))
                .andExpect(content().string(containsString("Teacher Sign In")))
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andExpect(content().string(containsString("type=\"submit\"")))
                .andExpect(content().string(containsString("</html>")));
    }

    @Test
    void getDashboardWithoutAuthenticationShouldRedirectToLogin() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void postLoginWithValidCredentialsShouldRedirectToDashboard() throws Exception {
        mockMvc.perform(formLogin("/login").user("duc.teacher").password("Password123!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    void postLoginWithInvalidCredentialsShouldRedirectToLoginError() throws Exception {
        mockMvc.perform(formLogin("/login").user("duc.teacher").password("WrongPassword123!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void postLogoutShouldRedirectToLoginLogout() throws Exception {
        mockMvc.perform(post("/logout").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout"));
    }

    @Test
    void authenticatedNonTeacherCannotReachTeacherWorkspaceRoutes() throws Exception {
        jdbcTemplate.update(
                """
                insert into users (username, password_hash, full_name, role, enabled)
                values (?, ?, ?, ?, ?)
                """,
                "linh.student",
                passwordEncoder.encode("Password123!"),
                "Tran Linh",
                "STUDENT",
                true
        );

        MvcResult loginResult = mockMvc.perform(formLogin("/login").user("linh.student").password("Password123!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);

        mockMvc.perform(get("/dashboard").session(session))
                .andExpect(status().isForbidden());
    }
}
