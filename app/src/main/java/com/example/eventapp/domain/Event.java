package com.example.eventapp.domain;

import java.time.LocalDate;

public record Event(
        long id,
        String title,
        String description,
        LocalDate startDate,
        LocalDate endDate,
        long cityId,
        long directionId,
        long organizerId,
        int capacity,
        String status
) {
}
