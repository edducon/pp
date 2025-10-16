package com.example.eventapp.model;

import java.time.LocalDateTime;

public class ActivityResource {
    private final long id;
    private final long activityId;
    private final String name;
    private final String resourcePath;
    private final Moderator uploadedBy;
    private final LocalDateTime uploadedAt;

    public ActivityResource(long id, long activityId, String name, String resourcePath, Moderator uploadedBy, LocalDateTime uploadedAt) {
        this.id = id;
        this.activityId = activityId;
        this.name = name;
        this.resourcePath = resourcePath;
        this.uploadedBy = uploadedBy;
        this.uploadedAt = uploadedAt;
    }

    public long getId() {
        return id;
    }

    public long getActivityId() {
        return activityId;
    }

    public String getName() {
        return name;
    }

    public String getResourcePath() {
        return resourcePath;
    }

    public Moderator getUploadedBy() {
        return uploadedBy;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }
}
