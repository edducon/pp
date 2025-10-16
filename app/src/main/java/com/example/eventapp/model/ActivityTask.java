package com.example.eventapp.model;

import java.time.LocalDateTime;

public class ActivityTask {
    private final long id;
    private final long activityId;
    private final String title;
    private final String description;
    private final Participant participant;
    private final LocalDateTime createdAt;

    public ActivityTask(long id, long activityId, String title, String description, Participant participant, LocalDateTime createdAt) {
        this.id = id;
        this.activityId = activityId;
        this.title = title;
        this.description = description;
        this.participant = participant;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public long getActivityId() {
        return activityId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Participant getParticipant() {
        return participant;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
