package com.example.eventapp;

import com.example.eventapp.config.Database;
import com.example.eventapp.config.DatabaseConfig;
import com.example.eventapp.dao.ActivityManagementDao;
import com.example.eventapp.dao.CityDao;
import com.example.eventapp.dao.DirectionDao;
import com.example.eventapp.dao.EventDao;
import com.example.eventapp.dao.JuryMemberDao;
import com.example.eventapp.dao.ModerationRequestDao;
import com.example.eventapp.dao.ModeratorDao;
import com.example.eventapp.dao.OrganizerDao;
import com.example.eventapp.dao.ParticipantDao;
import com.example.eventapp.service.AppContext;
import com.example.eventapp.service.AuthenticationService;
import com.example.eventapp.service.EventService;
import com.example.eventapp.service.ModeratorService;
import com.example.eventapp.service.RegistrationService;
import com.example.eventapp.ui.MainWindow;

import javax.swing.*;

public class EventApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            DatabaseConfig config = new DatabaseConfig();
            Database database = new Database(config);
            Runtime.getRuntime().addShutdownHook(new Thread(database::close));

            var dataSource = database.getDataSource();
            OrganizerDao organizerDao = new OrganizerDao(dataSource);
            ParticipantDao participantDao = new ParticipantDao(dataSource);
            ModeratorDao moderatorDao = new ModeratorDao(dataSource);
            JuryMemberDao juryMemberDao = new JuryMemberDao(dataSource);
            CityDao cityDao = new CityDao(dataSource);
            DirectionDao directionDao = new DirectionDao(dataSource);
            EventDao eventDao = new EventDao(dataSource);
            ModerationRequestDao moderationRequestDao = new ModerationRequestDao(dataSource);
            ActivityManagementDao activityManagementDao = new ActivityManagementDao(dataSource);

            AuthenticationService authenticationService = new AuthenticationService(organizerDao, participantDao, moderatorDao, juryMemberDao);
            EventService eventService = new EventService(eventDao, directionDao, cityDao, moderationRequestDao);
            RegistrationService registrationService = new RegistrationService(organizerDao, participantDao, moderatorDao, juryMemberDao, directionDao, cityDao);
            ModeratorService moderatorService = new ModeratorService(eventDao, moderationRequestDao);

            AppContext context = new AppContext(authenticationService, eventService, registrationService, moderatorService,
                    activityManagementDao, participantDao, juryMemberDao, moderatorDao, organizerDao, cityDao, directionDao, moderationRequestDao);

            MainWindow window = new MainWindow(context);
            window.setVisible(true);
        });
    }
}
