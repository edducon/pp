package com.example.eventapp.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class EventActivity {
    private final long id;
    private final long eventId;
    private final String title;
    private final String description;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final List<JuryMember> juryMembers = new ArrayList<>();
    private final List<ActivityTask> tasks = new ArrayList<>();
    private final List<ActivityResource> resources = new ArrayList<>();
    private String eventTitle;
    private Direction eventDirection;

    public EventActivity(long id, long eventId, String title, String description,
                         LocalDateTime startTime, LocalDateTime endTime) {
        this.id = id;
        this.eventId = eventId;
        this.title = title;
        this.description = description;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public long getId() {
        return id;
    }

    public long getEventId() {
        return eventId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public List<JuryMember> getJuryMembers() {
        return juryMembers;
    }

    public List<ActivityTask> getTasks() {
        return tasks;
    }

    public List<ActivityResource> getResources() {
        return resources;
    }

    public String getEventTitle() {
        return eventTitle;
    }

    public void setEventTitle(String eventTitle) {
        this.eventTitle = eventTitle;
    }

    public Direction getEventDirection() {
        return eventDirection;
    }

    public void setEventDirection(Direction eventDirection) {
        this.eventDirection = eventDirection;
    }

    @Override
    public String toString() {
        return "%s (%s-%s)".formatted(title, startTime.toLocalTime(), endTime.toLocalTime());
    }
}
