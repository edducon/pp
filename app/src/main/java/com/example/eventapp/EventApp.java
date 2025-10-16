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

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import javax.swing.SwingUtilities;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class EventApp extends Application {
    private Database database;
    private AppContext context;
    private MainWindow mainWindow;
    private Stage primaryStage;
    private volatile boolean shuttingDown;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        Platform.setImplicitExit(false);
        this.primaryStage = stage;

        DatabaseConfig config = new DatabaseConfig();
        this.database = new Database(config);

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

        this.context = new AppContext(authenticationService, eventService, registrationService, moderatorService,
                activityManagementDao, participantDao, juryMemberDao, moderatorDao, organizerDao, cityDao, directionDao, moderationRequestDao);

        Scene landingScene = buildLandingScene();
        stage.setTitle("Платформа управления мероприятиями");
        stage.setScene(landingScene);
        stage.setWidth(960);
        stage.setHeight(640);
        stage.centerOnScreen();
        stage.setOnCloseRequest(event -> {
            shuttingDown = true;
            if (mainWindow != null) {
                SwingUtilities.invokeLater(() -> mainWindow.dispose());
                mainWindow = null;
            }
            Platform.exit();
        });
        stage.show();
    }

    private Scene buildLandingScene() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: rgb(255,255,255);");

        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(16));
        header.setStyle("-fx-background-color: rgb(153,255,255);");

        Label headerTitle = new Label("Event Security Summit");
        headerTitle.setFont(Font.font("Comic Sans MS", FontWeight.BOLD, 32));
        headerTitle.setTextFill(Color.web("#0000CC"));

        header.getChildren().add(headerTitle);

        VBox contentCard = new VBox(24);
        contentCard.setAlignment(Pos.TOP_LEFT);
        contentCard.setPadding(new Insets(32));
        contentCard.setStyle("-fx-background-color: rgb(153,255,255); -fx-background-radius: 16;");

        Label greeting = new Label("Добро пожаловать на платформу управления мероприятиями по информационной безопасности!");
        greeting.setWrapText(true);
        greeting.setFont(Font.font("Comic Sans MS", FontWeight.BOLD, 24));
        greeting.setTextFill(Color.web("#0000CC"));

        Label description = new Label("Отслеживайте события, управляйте активностями и работайте с участниками, модераторами и жюри из единой панели.");
        description.setWrapText(true);
        description.setFont(Font.font("Comic Sans MS", 18));
        description.setTextFill(Color.web("#000000"));

        Button openButton = new Button("Открыть рабочий стол");
        openButton.setFont(Font.font("Comic Sans MS", FontWeight.BOLD, 20));
        openButton.setStyle("-fx-background-color: rgb(0,0,204); -fx-text-fill: white; -fx-padding: 12 32 12 32; -fx-background-radius: 12;");
        openButton.setOnAction(event -> openSwingWorkspace());

        contentCard.getChildren().addAll(greeting, description, openButton);

        VBox contentWrapper = new VBox();
        contentWrapper.setAlignment(Pos.TOP_CENTER);
        contentWrapper.setPadding(new Insets(48));
        contentWrapper.getChildren().add(contentCard);

        Region spacer = new Region();
        VBox.setVgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        root.setTop(header);
        root.setCenter(contentWrapper);
        root.setBottom(spacer);

        return new Scene(root);
    }

    private void openSwingWorkspace() {
        if (mainWindow == null) {
            SwingUtilities.invokeLater(() -> {
                MainWindow window = new MainWindow(context);
                window.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosed(WindowEvent e) {
                        mainWindow = null;
                        if (!shuttingDown) {
                            Platform.runLater(() -> {
                                if (primaryStage != null && !primaryStage.isShowing()) {
                                    primaryStage.show();
                                }
                            });
                        }
                    }
                });
                mainWindow = window;
                window.setVisible(true);
            });
        } else {
            SwingUtilities.invokeLater(() -> {
                mainWindow.toFront();
                mainWindow.requestFocus();
            });
        }
        if (primaryStage != null) {
            primaryStage.hide();
        }
    }

    @Override
    public void stop() {
        if (mainWindow != null) {
            SwingUtilities.invokeLater(() -> {
                mainWindow.dispose();
                mainWindow = null;
            });
        }
        if (database != null) {
            shuttingDown = true;
            database.close();
        }
    }
}
