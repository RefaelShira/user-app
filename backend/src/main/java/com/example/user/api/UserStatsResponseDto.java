package com.example.user.api;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode
@Builder
public class UserStatsResponseDto {
    private long createdLast24h;
}
