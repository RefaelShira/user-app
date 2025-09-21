package com.example.user.integrationtests;

import com.example.user.controller.ExceptionController;
import com.example.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;

import jakarta.annotation.Resource;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser
@Import(ExceptionController.class)
@TestPropertySource(properties = {
        "resilience4j.circuitbreaker.instances.dbOps.sliding-window-type=COUNT_BASED",
        "resilience4j.circuitbreaker.instances.dbOps.sliding-window-size=2",
        "resilience4j.circuitbreaker.instances.dbOps.minimum-number-of-calls=2",
        "resilience4j.circuitbreaker.instances.dbOps.failure-rate-threshold=50",
        "resilience4j.circuitbreaker.instances.dbOps.wait-duration-in-open-state=200ms",
        "resilience4j.retry.instances.dbOps.max-attempts=1"
        })
class CircuitBreakerIT {

    @Resource
    MockMvc mvc;

    @MockBean
    UserRepository repo;

    @Test
    void circuitOpensTest() throws Exception {
        when(repo.search(any(), anyBoolean(), any())).thenThrow(new DataAccessResourceFailureException("db down"));

        mvc.perform(get("/api/users")).andExpect(status().is5xxServerError());

        mvc.perform(get("/api/users")).andExpect(status().is5xxServerError());

        mvc.perform(get("/api/users")).andExpect(status().isServiceUnavailable());
    }
}
