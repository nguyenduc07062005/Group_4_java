package com.group4.javagrader;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
class PhaseOneUiMvcTests {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void dashboardRenders() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard/index"))
                .andExpect(content().string(containsString("Dashboard")));
    }

    @Test
    void semesterCreatePageRenders() throws Exception {
        mockMvc.perform(get("/semesters/create"))
                .andExpect(status().isOk())
                .andExpect(view().name("semester/create"))
                .andExpect(content().string(containsString("Create Semester")));
    }

    @Test
    void assignmentCreatePageRenders() throws Exception {
        mockMvc.perform(get("/assignments/create"))
                .andExpect(status().isOk())
                .andExpect(view().name("assignment/create"))
                .andExpect(content().string(containsString("Create Assignment")));
    }
}
