package com.example.eventapp.service;

import com.example.eventapp.repository.AccountRepository;
import com.example.eventapp.repository.CityRepository;
import com.example.eventapp.repository.DirectionRepository;
import com.example.eventapp.repository.EventRepository;
import com.example.eventapp.repository.EventTaskRepository;
import com.example.eventapp.repository.ModerationRequestRepository;
import com.example.eventapp.repository.ParticipantRegistrationRepository;

import javax.sql.DataSource;

public class AppContext {
    private final AuthenticationService authenticationService;
    private final EventService eventService;
    private final RegistrationService registrationService;
    private final ModerationService moderationService;
    private final DirectoryService directoryService;
    private final ReferenceDataService referenceDataService;

    public AppContext(DataSource dataSource) {
        AccountRepository accountRepository = new AccountRepository(dataSource);
        CityRepository cityRepository = new CityRepository(dataSource);
        DirectionRepository directionRepository = new DirectionRepository(dataSource);
        EventRepository eventRepository = new EventRepository(dataSource);
        EventTaskRepository taskRepository = new EventTaskRepository(dataSource);
        ModerationRequestRepository moderationRequestRepository = new ModerationRequestRepository(dataSource);
        ParticipantRegistrationRepository registrationRepository = new ParticipantRegistrationRepository(dataSource);

        this.authenticationService = new AuthenticationService(accountRepository);
        this.eventService = new EventService(eventRepository, taskRepository);
        this.registrationService = new RegistrationService(registrationRepository);
        this.moderationService = new ModerationService(moderationRequestRepository, eventRepository);
        this.directoryService = new DirectoryService(accountRepository);
        this.referenceDataService = new ReferenceDataService(cityRepository, directionRepository);
    }

    public AuthenticationService authenticationService() {
        return authenticationService;
    }

    public EventService eventService() {
        return eventService;
    }

    public RegistrationService registrationService() {
        return registrationService;
    }

    public ModerationService moderationService() {
        return moderationService;
    }

    public DirectoryService directoryService() {
        return directoryService;
    }

    public ReferenceDataService referenceDataService() {
        return referenceDataService;
    }
}
