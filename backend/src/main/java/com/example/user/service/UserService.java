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
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Slf4j
@Service
public class UserService {
    @Autowired
    private UserRepository repo;
    @Autowired
    private PasswordPolicy passwordPolicy;
    @Autowired
    private PasswordEncoder encoder;
    private static final int MAX_PAGE_SIZE = 100;
    @Retry(name = "dbOps")
    @CircuitBreaker(name = "dbOps")
    @Transactional
    public UserResponse create(CreateUserRequest req) {
        final String trimEmail = req.getEmail() == null ? null : req.getEmail().trim();

        passwordPolicy.validate(trimEmail, req.getPassword());

        repo.findByEmailIgnoreCase(trimEmail).ifPresent(u -> {
            throw new DuplicateRecordException("Email already exists");
        });

        UserEntity ent = UserEntity.builder()
                .firstName(req.getFirstName())
                .lastName(req.getLastName())
                .email(trimEmail)
                .passwordHash(encoder.encode(req.getPassword()))
                .active(true)
                .build();

        try {
            return UserEntity.toResponse(repo.save(ent));
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateRecordException("Email already exists");
        }
    }
    @Retry(name = "dbOps")
    @CircuitBreaker(name = "dbOps")
    @Transactional
    public Page<UserResponse> getList(String q, boolean activeOnly, int page, int size, String sort) {

        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size <= 0 ? 20 : size, MAX_PAGE_SIZE));


        Sort s = Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by("id").descending());
        if (sort != null && sort.contains(",")) {
            String[] parts = sort.split(",", 2);
            var dir = parts.length > 1 && "asc".equalsIgnoreCase(parts[1]) ? Sort.Direction.ASC : Sort.Direction.DESC;
            s = Sort.by(new Sort.Order(dir, parts[0]));
        }

        Pageable pageable = PageRequest.of(safePage, safeSize, s);
        return repo.search(q, activeOnly, pageable).map(UserEntity::toResponse);
    }
    @Retry(name = "dbOps")
    @CircuitBreaker(name = "dbOps")
    @Transactional
    public UUID delete(UUID id, boolean soft) {
        UserEntity ent = repo.findById(id).orElseThrow(() -> new NotFoundRecordException("User not found"));
        if (soft) {
            if (!ent.isActive()) {
                throw new UserAlreadyInactiveException("User Already inactive");
            }
            ent.setActive(false);
        } else {
            repo.delete(ent);
        }
        return id;
    }
    @Retry(name = "dbOps")
    @CircuitBreaker(name = "dbOps")
    @Transactional
    public UserStatsResponseDto getStats() {
        return UserStatsResponseDto.builder()
                .createdLast24h(repo.countUsersCreatedSince(Instant.now().minus(24, ChronoUnit.HOURS)))
                .build();
    }

}
