package com.example.eventapp.domain;

public record JuryMemberProfile(
        long id,
        String fullName,
        String achievements,
        String email
) {
}
