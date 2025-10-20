package com.example.eventapp.domain;

public record AuthenticatedUser(
        long id,
        String email,
        String fullName,
        Role role
) {
}
