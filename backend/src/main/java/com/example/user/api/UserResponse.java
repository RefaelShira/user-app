package com.example.user.api;

import jakarta.validation.constraints.Email;
import lombok.*;

import java.time.Instant;
import java.util.UUID;
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode
@Builder
public class UserResponse {
    private UUID id;
    private String firstName;
    private String lastName;
    @Email
    private String email;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}
