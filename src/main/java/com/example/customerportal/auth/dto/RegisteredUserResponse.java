package com.example.customerportal.auth.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RegisteredUserResponse(
        UUID id,
        String email,
        OffsetDateTime createdAt
) {
}
