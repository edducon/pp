package com.example.eventapp.model;

import java.time.LocalDateTime;

public class ModerationRequest {
    public enum Status {
        PENDING,
        APPROVED,
        DECLINED,
        CANCELLED
    }

    private final long id;
    private final EventActivity activity;
    private final Moderator moderator;
    private final Status status;
    private final Long conflictActivityId;
    private final String responseMessage;
    private final String declineReason;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public ModerationRequest(long id, EventActivity activity, Moderator moderator, Status status,
                             Long conflictActivityId, String responseMessage, String declineReason,
                             LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.activity = activity;
        this.moderator = moderator;
        this.status = status;
        this.conflictActivityId = conflictActivityId;
        this.responseMessage = responseMessage;
        this.declineReason = declineReason;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public long getId() {
        return id;
    }

    public EventActivity getActivity() {
        return activity;
    }

    public Moderator getModerator() {
        return moderator;
    }

    public Status getStatus() {
        return status;
    }

    public Long getConflictActivityId() {
        return conflictActivityId;
    }

    public String getResponseMessage() {
        return responseMessage;
    }

    public String getDeclineReason() {
        return declineReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
