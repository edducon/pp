package com.example.eventapp.domain;

public record ParticipantProfile(
        long id,
        String fullName,
        String company,
        String jobTitle,
        String email,
        String phone
) {
}
