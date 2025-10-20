package com.example.eventapp.domain;

import java.time.LocalDate;

public record EventDetails(
        long id,
        String title,
        String description,
        LocalDate startDate,
        LocalDate endDate,
        String cityName,
        String directionName,
        String status,
        int capacity,
        long organizerId
) {
}
