package com.example.eventapp.service;

import com.example.eventapp.dao.EventDao;
import com.example.eventapp.dao.ModerationRequestDao;
import com.example.eventapp.model.EventActivity;
import com.example.eventapp.model.ModerationRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class ModeratorService {
    private final EventDao eventDao;
    private final ModerationRequestDao moderationRequestDao;

    public ModeratorService(EventDao eventDao, ModerationRequestDao moderationRequestDao) {
        this.eventDao = eventDao;
        this.moderationRequestDao = moderationRequestDao;
    }

    public List<EventActivity> loadAllActivities() {
        return eventDao.findAllActivities();
    }

    public List<EventActivity> loadActivitiesForModerator(long moderatorId) {
        return eventDao.findActivitiesByModerator(moderatorId);
    }

    public List<ModerationRequest> loadModeratorRequests(long moderatorId) {
        return moderationRequestDao.findByModerator(moderatorId);
    }

    public Optional<ModerationRequest> findExistingRequest(long activityId, long moderatorId) {
        return moderationRequestDao.findActiveForModerator(activityId, moderatorId);
    }

    public List<ModerationRequest> findConflicts(long moderatorId, EventActivity activity) {
        return moderationRequestDao.findConflicts(moderatorId, activity.getStartTime(), activity.getEndTime());
    }

    public ModerationRequest submitRequest(long activityId, long moderatorId, ModerationRequest.Status status, Long conflictActivityId, String response) {
        return moderationRequestDao.create(activityId, moderatorId, status, conflictActivityId, response);
    }

    public void updateStatus(long requestId, ModerationRequest.Status status, String message, String declineReason) {
        moderationRequestDao.updateStatus(requestId, status, message, declineReason);
    }
}
