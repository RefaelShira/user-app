package com.example.user.controller;

import com.example.user.exceptions.DuplicateRecordException;
import com.example.user.exceptions.NotFoundRecordException;
import com.example.user.exceptions.UserAlreadyInactiveException;
import com.example.user.api.CreateUserRequest;
import com.example.user.api.UserResponse;
import com.example.user.api.UserStatsResponseDto;
import com.example.user.service.UserService;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(ExceptionController.class)
@WithMockUser
@WebMvcTest(controllers = UserController.class)
public class UserControllerTest {

    @Resource
    private MockMvc mvc;

    @MockBean
    private UserService userService;

    @Test
    @DisplayName("createSuccess")
    void createSuccessTest() throws Exception {
        var resp = UserResponse.builder()
                .id(UUID.randomUUID())
                .firstName("Israel")
                .lastName("Israeli")
                .email("israel@example.com")
                .active(true)
                .build();

        when(userService.create(any(CreateUserRequest.class))).thenReturn(resp);

        mvc.perform(post("/api/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Israel","lastName":"Israeli","email":"israel@example.com","password":"Secret1!"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.data.email").value("israel@example.com"));
    }

    @Test
    @DisplayName("createDuplicate")
    void createDuplicateTest() throws Exception {
        when(userService.create(any(CreateUserRequest.class)))
                .thenThrow(new DuplicateRecordException("Email already exists"));

        mvc.perform(post("/api/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Israel","lastName":"Israeli","email":"israel@example.com","password":"Secret1!"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("CONFLICT"));
    }

    @Test
    @DisplayName("badRequestRequiredFields")
    void badRequestRequiredFieldsTest() throws Exception {
        mvc.perform(post("/api/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                    {"firstName":"","lastName":""}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("badRequestNotEmail")
    void badRequestNotEmailTest() throws Exception {
        mvc.perform(post("/api/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                    {"firstName":"Israel","lastName":"Israeli","email":"not-an-email","password":"Secret1!"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("emptyBodyTest")
    void create_emptyBody_returns400() throws Exception {
        mvc.perform(post("/api/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("brokenJson")
    void brokenJsonTest() throws Exception {
        mvc.perform(post("/api/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"A\""))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("getList")
    void getListSuccessTest() throws Exception {
        var resp = UserResponse.builder()
                .id(UUID.randomUUID())
                .firstName("Israel")
                .lastName("Israeli")
                .email("israel@example.com")
                .active(true)
                .build();

        var page = new PageImpl<>(List.of(resp), PageRequest.of(0, 20), 1);

        when(userService.getList(eq("Isr"), eq(true), eq(0), eq(20), isNull()))
                .thenReturn(page);

        mvc.perform(get("/api/users")
                        .param("q", "Isr")
                        .param("page", "0")
                        .param("size", "20")
                        .param("activeOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].email").value("israel@example.com"))
                .andExpect(jsonPath("$.data.meta.totalElements").value(1));
    }

    @Test
    @DisplayName("serverUnavailable")
    void getListServerUnavailableTest() throws Exception {
        when(userService.getList(any(), anyBoolean(), anyInt(), anyInt(), any()))
                .thenThrow(CallNotPermittedException.createCallNotPermittedException(CircuitBreaker.ofDefaults("dbOps")));

        mvc.perform(get("/api/users")
                        .with(csrf()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("SERVICE_UNAVAILABLE"));
    }

    @Test
    @DisplayName("softDelete")
    void softDeleteTest() throws Exception {
        UUID id = UUID.randomUUID();
        when(userService.delete(eq(id), eq(true))).thenReturn(id);

        mvc.perform(delete("/api/users/{id}", id)
                        .with(csrf())
                        .param("soft", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.data").value(id.toString()));
    }

    @Test
    @DisplayName("deleteConflict")
    void deleteConflictTest() throws Exception {
        when(userService.delete(any(UUID.class), eq(true)))
                .thenThrow(new UserAlreadyInactiveException("already inactive"));

        mvc.perform(delete("/api/users/{id}", UUID.randomUUID())
                        .with(csrf())
                        .param("soft", "true"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value("ALREADY_INACTIVE"));
    }

    @Test
    @DisplayName("deleteNotFound")
    void deleteNotFoundTest() throws Exception {
        when(userService.delete(any(UUID.class), anyBoolean()))
                .thenThrow(new NotFoundRecordException("not found"));

        mvc.perform(delete("/api/users/{id}", UUID.randomUUID())
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("NOT_FOUND"));
    }


    @Test
    @DisplayName("getStats")
    void getStatsSuccessTest() throws Exception {
        when(userService.getStats())
                .thenReturn(UserStatsResponseDto.builder().createdLast24h(5L).build());

        mvc.perform(get("/api/users/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.data.createdLast24h").value(5));
    }

}
