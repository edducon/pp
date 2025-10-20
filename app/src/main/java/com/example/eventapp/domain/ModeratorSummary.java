package com.example.eventapp.domain;

public record ModeratorSummary(
        long id,
        String fullName,
        String expertise,
        String email
) {
}
