package com.example.eventapp.domain;

import java.time.LocalDateTime;

public record Account(
        long id,
        String email,
        String passwordHash,
        Role role,
        String firstName,
        String lastName,
        String middleName,
        String phone,
        LocalDateTime createdAt
) {
    public String fullName() {
        return String.join(" ",
                lastName != null ? lastName : "",
                firstName != null ? firstName : "",
                middleName != null ? middleName : "").trim();
    }
}
