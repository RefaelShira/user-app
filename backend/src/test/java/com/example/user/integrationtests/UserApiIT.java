package com.example.user.integrationtests;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.annotation.Resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser
class UserApiIT {

    @Resource MockMvc mvc;
    ObjectMapper om = new ObjectMapper();

    @Test
    void fullFlowTest() throws Exception {
        // Create
        var createRes = mvc.perform(post("/api/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
              {"firstName":"Israel","lastName":"Israeli","email":"israel@ex.com","password":"Secret1!"}
            """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andReturn();

        String id = om.readTree(createRes.getResponse().getContentAsString())
                .path("data").path("id").asText();
        assertThat(id).isNotBlank();

        // List with q & paging
        mvc.perform(get("/api/users")
                        .param("q","Israel").param("page","0").param("size","5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].email").value("israel@ex.com"))
                .andExpect(jsonPath("$.data.meta.totalElements").value(1));

        // Soft delete
        mvc.perform(delete("/api/users/{id}", id).with(csrf()).param("soft","true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(id));

        // Second delete
        mvc.perform(delete("/api/users/{id}", id).with(csrf()).param("soft","true"))
                .andExpect(status().isUnprocessableEntity());

        // Stats
        mvc.perform(get("/api/users/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.createdLast24h").isNumber());
    }
}
