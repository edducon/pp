package com.example.eventapp.domain;

import java.time.LocalDateTime;

public record ParticipantRegistration(
        long id,
        long eventId,
        long participantId,
        RegistrationStatus status,
        LocalDateTime createdAt
) {
}
