package com.example.eventapp.service;

import com.example.eventapp.domain.ModerationRequest;
import com.example.eventapp.domain.ModerationStatus;
import com.example.eventapp.repository.EventRepository;
import com.example.eventapp.repository.ModerationRequestRepository;

import java.util.List;

public class ModerationService {
    private final ModerationRequestRepository moderationRequestRepository;
    private final EventRepository eventRepository;

    public ModerationService(ModerationRequestRepository moderationRequestRepository, EventRepository eventRepository) {
        this.moderationRequestRepository = moderationRequestRepository;
        this.eventRepository = eventRepository;
    }

    public ModerationRequest sendForReview(long eventId, long organizerId, String message) {
        eventRepository.updateStatus(eventId, "PENDING_REVIEW");
        return moderationRequestRepository.create(eventId, organizerId, message);
    }

    public List<ModerationRequest> pendingRequests() {
        return moderationRequestRepository.findPending();
    }

    public List<ModerationRequest> requestsForModerator(long moderatorId) {
        return moderationRequestRepository.findByModerator(moderatorId);
    }

    public List<ModerationRequest> requestsForOrganizer(long organizerId) {
        return moderationRequestRepository.findByOrganizer(organizerId);
    }

    public void assignToModerator(long requestId, long moderatorId) {
        moderationRequestRepository.assignModerator(requestId, moderatorId);
    }

    public void approve(long requestId, long eventId, String message) {
        moderationRequestRepository.updateStatus(requestId, ModerationStatus.APPROVED, message);
        eventRepository.updateStatus(eventId, "APPROVED");
    }

    public void reject(long requestId, long eventId, String message) {
        moderationRequestRepository.updateStatus(requestId, ModerationStatus.REJECTED, message);
        eventRepository.updateStatus(eventId, "REJECTED");
    }
}
