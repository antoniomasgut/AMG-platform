package com.amg.digitalitzacio.demo.api;

import com.amg.digitalitzacio.shared.config.TestRedisConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
class DemoControllerTest {

    @Autowired private MockMvc mockMvc;

    @Test
    void listDemos_returnsAllFlows() throws Exception {
        mockMvc.perform(get("/api/v1/demo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.demos", hasSize(4)))
                .andExpect(jsonPath("$.demos[0].id").isString())
                .andExpect(jsonPath("$.demos[0].title").isString());
    }

    @Test
    void getDemo_newLead_returnsFlow() throws Exception {
        mockMvc.perform(get("/api/v1/demo/new-lead"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("new-lead"))
                .andExpect(jsonPath("$.title").isString())
                .andExpect(jsonPath("$.steps", hasSize(5)))
                .andExpect(jsonPath("$.steps[0].action").isString());
    }

    @Test
    void getDemo_budgetEmail_returnsFlow() throws Exception {
        mockMvc.perform(get("/api/v1/demo/budget-email"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("budget-email"))
                .andExpect(jsonPath("$.steps", hasSize(5)));
    }

    @Test
    void getDemo_unknownId_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/demo/no-existeix"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getDemo_noAuthRequired() throws Exception {
        mockMvc.perform(get("/api/v1/demo/new-lead"))
                .andExpect(status().isOk());
    }
}
