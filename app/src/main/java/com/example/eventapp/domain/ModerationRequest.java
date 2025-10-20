package com.example.eventapp.domain;

import java.time.LocalDateTime;

public record ModerationRequest(
        long id,
        long eventId,
        long organizerId,
        Long moderatorId,
        ModerationStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String message
) {
}
