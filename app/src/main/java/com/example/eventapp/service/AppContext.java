package com.example.eventapp.service;

import com.example.eventapp.dao.ActivityManagementDao;
import com.example.eventapp.dao.CityDao;
import com.example.eventapp.dao.DirectionDao;
import com.example.eventapp.dao.JuryMemberDao;
import com.example.eventapp.dao.ModerationRequestDao;
import com.example.eventapp.dao.ModeratorDao;
import com.example.eventapp.dao.OrganizerDao;
import com.example.eventapp.dao.ParticipantDao;

public class AppContext {
    private final AuthenticationService authenticationService;
    private final EventService eventService;
    private final RegistrationService registrationService;
    private final ModeratorService moderatorService;
    private final ActivityManagementDao activityManagementDao;
    private final ParticipantDao participantDao;
    private final JuryMemberDao juryMemberDao;
    private final ModeratorDao moderatorDao;
    private final OrganizerDao organizerDao;
    private final CityDao cityDao;
    private final DirectionDao directionDao;
    private final ModerationRequestDao moderationRequestDao;

    public AppContext(AuthenticationService authenticationService,
                      EventService eventService,
                      RegistrationService registrationService,
                      ModeratorService moderatorService,
                      ActivityManagementDao activityManagementDao,
                      ParticipantDao participantDao,
                      JuryMemberDao juryMemberDao,
                      ModeratorDao moderatorDao,
                      OrganizerDao organizerDao,
                      CityDao cityDao,
                      DirectionDao directionDao,
                      ModerationRequestDao moderationRequestDao) {
        this.authenticationService = authenticationService;
        this.eventService = eventService;
        this.registrationService = registrationService;
        this.moderatorService = moderatorService;
        this.activityManagementDao = activityManagementDao;
        this.participantDao = participantDao;
        this.juryMemberDao = juryMemberDao;
        this.moderatorDao = moderatorDao;
        this.organizerDao = organizerDao;
        this.cityDao = cityDao;
        this.directionDao = directionDao;
        this.moderationRequestDao = moderationRequestDao;
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

    public ModeratorService moderatorService() {
        return moderatorService;
    }

    public ActivityManagementDao activityManagementDao() {
        return activityManagementDao;
    }

    public ParticipantDao participantDao() {
        return participantDao;
    }

    public JuryMemberDao juryMemberDao() {
        return juryMemberDao;
    }

    public ModeratorDao moderatorDao() {
        return moderatorDao;
    }

    public OrganizerDao organizerDao() {
        return organizerDao;
    }

    public CityDao cityDao() {
        return cityDao;
    }

    public DirectionDao directionDao() {
        return directionDao;
    }

    public ModerationRequestDao moderationRequestDao() {
        return moderationRequestDao;
    }
}
