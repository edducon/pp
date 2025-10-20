package com.example.eventapp.domain;

import java.time.LocalDate;

public record EventTask(
        long id,
        long eventId,
        String stage,
        String title,
        TaskStatus status,
        LocalDate dueDate,
        String assignee,
        String notes
) {
}
