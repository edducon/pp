package com.example.eventapp.service;

import com.example.eventapp.domain.ParticipantRegistration;
import com.example.eventapp.domain.RegistrationStatus;
import com.example.eventapp.repository.ParticipantRegistrationRepository;

import java.util.List;

public class RegistrationService {
    private final ParticipantRegistrationRepository registrationRepository;

    public RegistrationService(ParticipantRegistrationRepository registrationRepository) {
        this.registrationRepository = registrationRepository;
    }

    public ParticipantRegistration requestParticipation(long eventId, long participantId) {
        return registrationRepository.create(eventId, participantId);
    }

    public void approve(long registrationId) {
        registrationRepository.updateStatus(registrationId, RegistrationStatus.APPROVED);
    }

    public void decline(long registrationId) {
        registrationRepository.updateStatus(registrationId, RegistrationStatus.DECLINED);
    }

    public List<ParticipantRegistration> registrationsForEvent(long eventId) {
        return registrationRepository.findByEvent(eventId);
    }
}
