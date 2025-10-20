package com.example.eventapp.ui;

import com.example.eventapp.domain.AuthenticatedUser;
import com.example.eventapp.service.AuthenticationService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.function.Consumer;

public class LoginView extends BorderPane {
    private final AuthenticationService authenticationService;
    private final Consumer<AuthenticatedUser> onLogin;

    public LoginView(AuthenticationService authenticationService, Consumer<AuthenticatedUser> onLogin) {
        this.authenticationService = authenticationService;
        this.onLogin = onLogin;
        setPadding(new Insets(24));
        setStyle("-fx-background-color: linear-gradient(to right, #a8edea, #fed6e3);");

        VBox content = new VBox(24);
        content.setPadding(new Insets(32));
        content.setMaxWidth(720);
        content.setAlignment(Pos.TOP_CENTER);
        content.setStyle("-fx-background-color: rgba(255,255,255,0.9); -fx-background-radius: 16;");

        Label title = new Label("Event Security Summit");
        title.setFont(Font.font("Comic Sans MS", FontWeight.BOLD, 32));
        title.setTextFill(Color.web("#0055A4"));

        Label subtitle = new Label("Управляйте заявками, модерацией и регистрацией участников в едином окне");
        subtitle.setWrapText(true);
        subtitle.setFont(Font.font("Comic Sans MS", 18));
        subtitle.setTextFill(Color.web("#333333"));

        VBox loginBox = buildLoginBox();
        TabPane registrationTabs = buildRegistrationTabs();

        content.getChildren().addAll(title, subtitle, loginBox, registrationTabs);
        setCenter(content);
    }

    private VBox buildLoginBox() {
        VBox wrapper = new VBox(16);
        wrapper.setAlignment(Pos.CENTER);

        Label caption = new Label("Вход для существующих пользователей");
        caption.setFont(Font.font("Comic Sans MS", FontWeight.BOLD, 18));

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setAlignment(Pos.CENTER);

        Label emailLabel = new Label("Email:");
        TextField emailField = new TextField();
        emailField.setPromptText("user@example.com");

        Label passwordLabel = new Label("Пароль:");
        PasswordField passwordField = new PasswordField();

        Label messageLabel = new Label();
        messageLabel.setTextFill(Color.web("#B00020"));

        Button loginButton = new Button("Войти");
        loginButton.setDefaultButton(true);
        loginButton.setOnAction(event -> {
            String email = emailField.getText().trim();
            String password = passwordField.getText();
            authenticationService.login(email, password)
                    .ifPresentOrElse(user -> {
                        messageLabel.setText("");
                        onLogin.accept(user);
                    }, () -> messageLabel.setText("Неверная пара email/пароль"));
        });

        grid.addRow(0, emailLabel, emailField);
        grid.addRow(1, passwordLabel, passwordField);
        grid.add(loginButton, 1, 2);
        GridPane.setMargin(loginButton, new Insets(8, 0, 0, 0));

        wrapper.getChildren().addAll(caption, grid, messageLabel);
        return wrapper;
    }

    private TabPane buildRegistrationTabs() {
        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getTabs().add(createParticipantTab());
        tabs.getTabs().add(createOrganizerTab());
        tabs.getTabs().add(createModeratorTab());
        tabs.getTabs().add(createJuryTab());
        return tabs;
    }

    private Tab createParticipantTab() {
        return buildRegistrationTab("Участник", (fields) -> {
            authenticationService.registerParticipant(
                    fields.email.getText(),
                    fields.password.getText(),
                    fields.firstName.getText(),
                    fields.lastName.getText(),
                    fields.middleName.getText(),
                    fields.phone.getText(),
                    fields.extraFieldOne.getText(),
                    fields.extraFieldTwo.getText());
        }, "Компания", "Должность");
    }

    private Tab createOrganizerTab() {
        return buildRegistrationTab("Организатор", (fields) -> {
            authenticationService.registerOrganizer(
                    fields.email.getText(),
                    fields.password.getText(),
                    fields.firstName.getText(),
                    fields.lastName.getText(),
                    fields.middleName.getText(),
                    fields.phone.getText(),
                    fields.extraFieldOne.getText(),
                    fields.extraFieldTwo.getText());
        }, "Компания", "Веб-сайт");
    }

    private Tab createModeratorTab() {
        return buildRegistrationTab("Модератор", (fields) -> {
            authenticationService.registerModerator(
                    fields.email.getText(),
                    fields.password.getText(),
                    fields.firstName.getText(),
                    fields.lastName.getText(),
                    fields.middleName.getText(),
                    fields.phone.getText(),
                    fields.extraFieldOne.getText());
        }, "Экспертиза", null);
    }

    private Tab createJuryTab() {
        return buildRegistrationTab("Член жюри", (fields) -> {
            authenticationService.registerJury(
                    fields.email.getText(),
                    fields.password.getText(),
                    fields.firstName.getText(),
                    fields.lastName.getText(),
                    fields.middleName.getText(),
                    fields.phone.getText(),
                    fields.extraFieldOne.getText());
        }, "Достижения", null);
    }

    private Tab buildRegistrationTab(String title, RegistrationHandler handler, String extraFieldOneLabel, String extraFieldTwoLabel) {
        Tab tab = new Tab(title);
        VBox root = new VBox(16);
        root.setPadding(new Insets(16));
        root.setAlignment(Pos.TOP_CENTER);

        Label caption = new Label("Регистрация роли: " + title);
        caption.setFont(Font.font("Comic Sans MS", FontWeight.BOLD, 18));

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);

        TextField emailField = new TextField();
        TextField firstNameField = new TextField();
        TextField lastNameField = new TextField();
        TextField middleNameField = new TextField();
        TextField phoneField = new TextField();
        PasswordField passwordField = new PasswordField();
        PasswordField confirmPasswordField = new PasswordField();
        TextField extraOne = extraFieldOneLabel != null ? new TextField() : null;
        TextField extraTwo = extraFieldTwoLabel != null ? new TextField() : null;

        grid.addRow(0, new Label("Email:"), emailField);
        grid.addRow(1, new Label("Имя:"), firstNameField);
        grid.addRow(2, new Label("Фамилия:"), lastNameField);
        grid.addRow(3, new Label("Отчество:"), middleNameField);
        grid.addRow(4, new Label("Телефон:"), phoneField);
        grid.addRow(5, new Label("Пароль:"), passwordField);
        grid.addRow(6, new Label("Подтверждение:"), confirmPasswordField);
        int row = 7;
        if (extraOne != null) {
            grid.addRow(row++, new Label(extraFieldOneLabel + ":"), extraOne);
        }
        if (extraTwo != null) {
            grid.addRow(row++, new Label(extraFieldTwoLabel + ":"), extraTwo);
        }

        Label messageLabel = new Label();
        messageLabel.setTextFill(Color.web("#388E3C"));

        Button registerButton = new Button("Зарегистрироваться");
        registerButton.setOnAction(event -> {
            if (!passwordField.getText().equals(confirmPasswordField.getText())) {
                messageLabel.setTextFill(Color.web("#B00020"));
                messageLabel.setText("Пароли не совпадают");
                return;
            }
            try {
                handler.handle(new RegistrationFields(emailField, passwordField, firstNameField, lastNameField,
                        middleNameField, phoneField, extraOne, extraTwo));
                messageLabel.setTextFill(Color.web("#388E3C"));
                messageLabel.setText("Регистрация выполнена. Используйте форму входа выше.");
                clearFields(emailField, firstNameField, lastNameField, middleNameField, phoneField, passwordField, confirmPasswordField, extraOne, extraTwo);
            } catch (RuntimeException ex) {
                messageLabel.setTextFill(Color.web("#B00020"));
                messageLabel.setText("Ошибка регистрации: " + ex.getMessage());
            }
        });

        HBox buttonBox = new HBox(registerButton);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        root.getChildren().addAll(caption, grid, buttonBox, messageLabel);
        VBox.setVgrow(grid, Priority.ALWAYS);
        tab.setContent(root);
        return tab;
    }

    private void clearFields(TextField... fields) {
        if (fields == null) {
            return;
        }
        for (TextField field : fields) {
            if (field != null) {
                field.clear();
            }
        }
    }

    private record RegistrationFields(TextField email,
                                       PasswordField password,
                                       TextField firstName,
                                       TextField lastName,
                                       TextField middleName,
                                       TextField phone,
                                       TextField extraFieldOne,
                                       TextField extraFieldTwo) {
    }

    @FunctionalInterface
    private interface RegistrationHandler {
        void handle(RegistrationFields fields);
    }
}
