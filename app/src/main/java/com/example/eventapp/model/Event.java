package com.example.eventapp.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Event {
    private final long id;
    private final long organizerId;
    private final String title;
    private final Direction direction;
    private final String description;
    private final String logoPath;
    private final int cityId;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final List<EventActivity> activities = new ArrayList<>();

    public Event(long id, long organizerId, String title, Direction direction, String description,
                 String logoPath, int cityId, LocalDateTime startTime, LocalDateTime endTime) {
        this.id = id;
        this.organizerId = organizerId;
        this.title = title;
        this.direction = direction;
        this.description = description;
        this.logoPath = logoPath;
        this.cityId = cityId;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public long getId() {
        return id;
    }

    public long getOrganizerId() {
        return organizerId;
    }

    public String getTitle() {
        return title;
    }

    public Direction getDirection() {
        return direction;
    }

    public String getDescription() {
        return description;
    }

    public String getLogoPath() {
        return logoPath;
    }

    public int getCityId() {
        return cityId;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public List<EventActivity> getActivities() {
        return activities;
    }
}
