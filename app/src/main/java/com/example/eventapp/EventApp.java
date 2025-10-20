package com.example.eventapp;

import com.example.eventapp.config.DatabaseConfig;
import com.example.eventapp.db.Database;
import com.example.eventapp.domain.AuthenticatedUser;
import com.example.eventapp.service.AppContext;
import com.example.eventapp.ui.LoginView;
import com.example.eventapp.ui.MainDashboard;
import com.example.eventapp.util.SqlScriptRunner;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

public class EventApp extends Application {
    private Database database;
    private AppContext context;
    private Stage primaryStage;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        DatabaseConfig config = new DatabaseConfig();
        this.database = new Database(config);
        if (!schemaExists()) {
            SqlScriptRunner.runScript(database.getDataSource(), "db/schema.sql");
        }
        this.context = new AppContext(database.getDataSource());

        LoginView loginView = new LoginView(context.authenticationService(), this::openDashboard);
        Scene scene = new Scene(loginView, 960, 640);
        stage.setTitle("Event Security Summit Platform");
        stage.setScene(scene);
        stage.show();
        stage.setOnCloseRequest(event -> shutdown());
    }

    private void openDashboard(AuthenticatedUser user) {
        MainDashboard dashboard = new MainDashboard(user, context, this::showLogin);
        Scene dashboardScene = new Scene(dashboard, 1200, 720);
        primaryStage.setScene(dashboardScene);
        primaryStage.centerOnScreen();
    }

    private void showLogin() {
        LoginView loginView = new LoginView(context.authenticationService(), this::openDashboard);
        primaryStage.setScene(new Scene(loginView, 960, 640));
        primaryStage.centerOnScreen();
    }

    private void shutdown() {
        Platform.exit();
        if (database != null) {
            database.close();
        }
    }

    private boolean schemaExists() {
        try (Connection connection = database.getDataSource().getConnection();
             ResultSet tables = connection.getMetaData().getTables(connection.getCatalog(), null, "accounts", null)) {
            return tables.next();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to verify schema", e);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
