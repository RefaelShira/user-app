package com.example.user.service;

import com.example.user.exceptions.DuplicateRecordException;
import com.example.user.exceptions.NotFoundRecordException;
import com.example.user.exceptions.UserAlreadyInactiveException;
import com.example.user.api.CreateUserRequest;
import com.example.user.api.UserResponse;
import com.example.user.api.UserStatsResponseDto;
import com.example.user.domain.PasswordPolicy;
import com.example.user.entity.UserEntity;
import com.example.user.repository.UserRepository;
import org.junit.jupiter.api.*;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    @Mock
    UserRepository repo;
    @Mock
    PasswordPolicy passwordPolicy;
    @Mock
    PasswordEncoder encoder;

    @InjectMocks
    UserService service;
    private UUID id;
    private UserEntity user;

    @BeforeEach
    void init() {
        user = getUserEntity("Israel", "Israeli", "israel@example.com", true, 0L);
    }
    @Test
    @DisplayName("createSuccess")
    void createSuccessTest(){
        when(repo.findByEmailIgnoreCase("israel@example.com")).thenReturn(Optional.empty());
        when(encoder.encode("Secret1!")).thenReturn("sct");
        when(repo.save(any(UserEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        CreateUserRequest req = CreateUserRequest.builder()
                .firstName("Israel")
                .lastName("Israeli")
                .email("   israel@example.com   ") //for check trim
                .password("Secret1!")
                .build();

        UserResponse result = service.create(req);
        verify(passwordPolicy).validate("israel@example.com", "Secret1!");
        ArgumentCaptor<UserEntity> saved = ArgumentCaptor.forClass(UserEntity.class);
        verify(repo).save(saved.capture());
        assertThat(saved.getValue().getEmail()).isEqualTo("israel@example.com");
        assertThat(saved.getValue().getPasswordHash()).isEqualTo("sct");
        assertThat(saved.getValue().isActive()).isTrue();
        assertThat(result).isNotNull();
    }
    @Test
    @DisplayName("createDuplicateEmail")
    void createDuplicateEmailTest() {
        when(repo.findByEmailIgnoreCase("israel@example.com")).thenReturn(Optional.of(user));

        CreateUserRequest req = CreateUserRequest.builder()
                .firstName("Israel")
                .lastName("Israeli")
                .email("israel@example.com")
                .password("Secret1!")
                .build();

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(DuplicateRecordException.class);

        verify(repo, never()).save(any());
    }

    @Test
    @DisplayName("createDoubleDB")
    void createDoubleDBTest() {
        when(repo.findByEmailIgnoreCase("israel@example.com")).thenReturn(Optional.empty());
        when(encoder.encode(anyString())).thenReturn("sct");
        when(repo.save(any(UserEntity.class))).thenThrow(new DataIntegrityViolationException("uq email"));

        CreateUserRequest req = CreateUserRequest.builder()
                .firstName("Israel")
                .lastName("Israeli")
                .email("israel@example.com")
                .password("Secret1!")
                .build();

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(DuplicateRecordException.class);
    }

    @Test
    @DisplayName("getList")
    void getListSuccessTest() {
        ArgumentCaptor<Pageable> pageableCap = ArgumentCaptor.forClass(Pageable.class);

        when(repo.search(any(), anyBoolean(), any(Pageable.class)))
                .thenAnswer(inv -> {
                    Pageable p = inv.getArgument(2);
                    return new PageImpl<>(List.of(user), p, 1);
                });

        Page<UserResponse> res = service.getList("Israel", true, -5, -1, "firstName,asc");
        assertThat(res.getTotalElements()).isEqualTo(1);

        verify(repo).search(eq("Israel"), eq(true), pageableCap.capture());
        Pageable used = pageableCap.getValue();
        assertThat(used.getPageNumber()).isEqualTo(0);
        assertThat(used.getPageSize()).isEqualTo(20);
        var orders = used.getSort().toList();
        assertThat(orders).hasSize(1);
        assertThat(orders.get(0).getProperty()).isEqualTo("firstName");
        assertThat(orders.get(0).isAscending()).isTrue();
    }

    @Test
    @DisplayName("getListMaxPageSize")
    void getListMaxPageSizeTest() {
        ArgumentCaptor<Pageable> pageableCap = ArgumentCaptor.forClass(Pageable.class);
        when(repo.search(any(), anyBoolean(), any(Pageable.class)))
                .thenAnswer(inv -> new PageImpl<>(List.of(user), inv.getArgument(2), 1));

        service.getList(null, false, 3, 1000, null);

        verify(repo).search(isNull(), eq(false), pageableCap.capture());
        assertThat(pageableCap.getValue().getPageNumber()).isEqualTo(3);
        assertThat(pageableCap.getValue().getPageSize()).isEqualTo(100); // MAX_PAGE_SIZE
    }

    @Test
    @DisplayName("SoftDelete")
    void softDeleteTest() {
        when(repo.findById(id)).thenReturn(Optional.of(user));

        UUID out = service.delete(id, true);

        assertThat(out).isEqualTo(id);
        assertThat(user.isActive()).isFalse();
        verify(repo, never()).delete(user);
    }

    @Test
    @DisplayName("deleteAgainConflict")
    void deleteAgainConflictTest() {
        UserEntity inactiveUser = getUserEntity("Israel", "Israeli", "israel@example.com", false, 0L);
        when(repo.findById(id)).thenReturn(Optional.of(inactiveUser));

        assertThatThrownBy(() -> service.delete(id, true))
                .isInstanceOf(UserAlreadyInactiveException.class);

        verify(repo, never()).delete(user);
    }

    @Test
    @DisplayName("deleteHard")
    void deleteHardTest() {
        when(repo.findById(id)).thenReturn(Optional.of(user));

        UUID out = service.delete(id, false);

        assertThat(out).isEqualTo(id);
        verify(repo).delete(user);
    }

    @Test
    @DisplayName("deleteNotFound")
    void deleteNotFoundTest() {
        when(repo.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(id, true))
                .isInstanceOf(NotFoundRecordException.class);
    }

    // ---------------- getStats ----------------

    @Test
    @DisplayName("getStats")
    void getStatsTest() {
        when(repo.countUsersCreatedSince(any(Instant.class))).thenReturn(7L);

        UserStatsResponseDto dto = service.getStats();
        assertThat(dto.getCreatedLast24h()).isEqualTo(7);

        ArgumentCaptor<Instant> sinceCap = ArgumentCaptor.forClass(Instant.class);
        verify(repo).countUsersCreatedSince(sinceCap.capture());

        Instant since = sinceCap.getValue();
        Instant nowMinus24h = Instant.now().minus(Duration.ofHours(24));
        assertThat(Duration.between(since, nowMinus24h).abs()).isLessThanOrEqualTo(Duration.ofSeconds(5));
    }

    private static UserEntity getUserEntity(
            String firstName, String lastName, String email,
            boolean active, Long version) {
        return UserEntity.builder()
                .id(UUID.randomUUID())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .active(active)
                .version(version)
                .passwordHash("passwordHash")
                .build();
    }
}
