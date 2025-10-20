package com.example.eventapp.ui;

import com.example.eventapp.domain.AuthenticatedUser;
import com.example.eventapp.domain.EventDetails;
import com.example.eventapp.domain.EventTask;
import com.example.eventapp.domain.ModerationRequest;
import com.example.eventapp.domain.ParticipantProfile;
import com.example.eventapp.domain.Role;
import com.example.eventapp.domain.TaskStatus;
import com.example.eventapp.service.AppContext;
import com.example.eventapp.ui.components.EventFormDialog;
import com.example.eventapp.ui.components.TaskDialog;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.stream.Collectors;

public class MainDashboard extends BorderPane {
    private final AuthenticatedUser user;
    private final AppContext context;
    private final Runnable onLogout;

    private final ObservableList<EventDetails> organizerEvents = FXCollections.observableArrayList();
    private final ObservableList<ModerationRequest> moderationRequests = FXCollections.observableArrayList();
    private final ObservableList<EventDetails> catalogEvents = FXCollections.observableArrayList();
    private final ObservableList<EventTask> selectedEventTasks = FXCollections.observableArrayList();
    private EventDetails currentOrganizerEvent;

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy");

    public MainDashboard(AuthenticatedUser user, AppContext context, Runnable onLogout) {
        this.user = user;
        this.context = context;
        this.onLogout = onLogout;
        setPadding(new Insets(24));
        setStyle("-fx-background-color: linear-gradient(to bottom, #f8ffff, #e0f7fa);");

        setTop(buildHeader());

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.getTabs().add(buildCatalogTab());

        if (user.role() == Role.ORGANIZER) {
            tabPane.getTabs().add(buildOrganizerEventsTab());
            tabPane.getTabs().add(buildOrganizerModerationTab());
            tabPane.getTabs().add(buildParticipantsDirectoryTab());
        }
        if (user.role() == Role.MODERATOR) {
            tabPane.getTabs().add(buildModeratorRequestsTab());
            tabPane.getTabs().add(buildTaskBoardTab());
        }
        if (user.role() == Role.PARTICIPANT) {
            tabPane.getTabs().add(buildParticipantsDirectoryTab());
        }
        if (user.role() == Role.JURY) {
            tabPane.getTabs().add(buildParticipantsDirectoryTab());
        }

        setCenter(tabPane);
        refreshCatalog();
        if (user.role() == Role.ORGANIZER) {
            refreshOrganizerData();
        }
        if (user.role() == Role.MODERATOR) {
            refreshModeratorData();
        }
    }

    private BorderPane buildHeader() {
        BorderPane header = new BorderPane();
        header.setPadding(new Insets(16));
        header.setStyle("-fx-background-color: rgba(0,85,164,0.9); -fx-background-radius: 12;");

        Label title = new Label("Панель управления");
        title.setFont(Font.font("Comic Sans MS", FontWeight.BOLD, 26));
        title.setTextFill(Color.WHITE);

        Label userLabel = new Label(user.fullName() + " · " + user.role());
        userLabel.setTextFill(Color.WHITE);
        userLabel.setFont(Font.font("Comic Sans MS", 16));

        Button logoutButton = new Button("Выйти");
        logoutButton.setOnAction(event -> onLogout.run());

        header.setLeft(title);
        BorderPane.setAlignment(title, Pos.CENTER_LEFT);
        header.setCenter(userLabel);
        BorderPane.setAlignment(userLabel, Pos.CENTER);
        header.setRight(logoutButton);
        BorderPane.setAlignment(logoutButton, Pos.CENTER_RIGHT);
        return header;
    }

    private Tab buildCatalogTab() {
        Tab tab = new Tab("Каталог мероприятий");
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(16));

        TableView<EventDetails> table = createEventTable();
        table.setItems(catalogEvents);

        Button refreshButton = new Button("Обновить каталог");
        refreshButton.setOnAction(event -> refreshCatalog());

        VBox topBox = new VBox(8, new Label("Предстоящие события"), refreshButton);
        root.setTop(topBox);
        root.setCenter(table);

        if (user.role() == Role.PARTICIPANT) {
            Label messageLabel = new Label();
            messageLabel.setTextFill(Color.web("#388E3C"));
            Button registerButton = new Button("Оставить заявку на участие");
            registerButton.setOnAction(event -> {
                EventDetails selected = table.getSelectionModel().getSelectedItem();
                if (selected == null) {
                    messageLabel.setTextFill(Color.web("#B00020"));
                    messageLabel.setText("Выберите мероприятие");
                    return;
                }
                var registration = context.registrationService().requestParticipation(selected.id(), user.id());
                messageLabel.setTextFill(Color.web("#388E3C"));
                messageLabel.setText("Заявка отправлена. Статус: " + registration.status());
            });
            VBox bottom = new VBox(8, registerButton, messageLabel);
            bottom.setAlignment(Pos.CENTER_LEFT);
            bottom.setPadding(new Insets(12, 0, 0, 0));
            root.setBottom(bottom);
        }

        tab.setContent(root);
        return tab;
    }

    private TableView<EventDetails> createEventTable() {
        TableView<EventDetails> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TableColumn<EventDetails, String> titleCol = new TableColumn<>("Название");
        titleCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().title()));

        TableColumn<EventDetails, String> datesCol = new TableColumn<>("Даты");
        datesCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                data.getValue().startDate().format(dateFormatter) + " – " + data.getValue().endDate().format(dateFormatter)));

        TableColumn<EventDetails, String> cityCol = new TableColumn<>("Город");
        cityCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().cityName()));

        TableColumn<EventDetails, String> directionCol = new TableColumn<>("Направление");
        directionCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().directionName()));

        TableColumn<EventDetails, String> statusCol = new TableColumn<>("Статус");
        statusCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().status()));

        TableColumn<EventDetails, Number> capacityCol = new TableColumn<>("Вместимость");
        capacityCol.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().capacity()));

        table.getColumns().addAll(titleCol, datesCol, cityCol, directionCol, statusCol, capacityCol);
        return table;
    }

    private Tab buildOrganizerEventsTab() {
        Tab tab = new Tab("Мои мероприятия");
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(16));

        TableView<EventDetails> table = createEventTable();
        table.setItems(organizerEvents);
        organizerEvents.addListener((javafx.collections.ListChangeListener<EventDetails>) change -> {
            if (!organizerEvents.isEmpty()) {
                table.getSelectionModel().select(organizerEvents.get(0));
            }
        });

        Button newEventButton = new Button("Создать мероприятие");
        newEventButton.setOnAction(event -> openEventDialog());

        Button sendToModeration = new Button("Отправить на модерацию");
        sendToModeration.setOnAction(event -> {
            EventDetails selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                context.moderationService().sendForReview(selected.id(), user.id(), "Запрос подтверждения программы");
                refreshOrganizerData();
            }
        });

        Button refreshButton = new Button("Обновить");
        refreshButton.setOnAction(event -> refreshOrganizerData());

        HBox controls = new HBox(12, newEventButton, sendToModeration, refreshButton);
        controls.setAlignment(Pos.CENTER_LEFT);

        VBox container = new VBox(12, controls, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        root.setCenter(container);

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            currentOrganizerEvent = newVal;
            if (newVal != null) {
                loadTasksForEvent(newVal.id());
            } else {
                selectedEventTasks.clear();
            }
        });

        root.setBottom(buildTasksPane());
        tab.setContent(root);
        return tab;
    }

    private VBox buildTasksPane() {
        Label caption = new Label("Задачи мероприятия");
        caption.setFont(Font.font("Comic Sans MS", FontWeight.BOLD, 16));

        TableView<EventTask> taskTable = new TableView<>(selectedEventTasks);
        taskTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        TableColumn<EventTask, String> stageCol = new TableColumn<>("Этап");
        stageCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().stage()));
        TableColumn<EventTask, String> titleCol = new TableColumn<>("Название");
        titleCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().title()));
        TableColumn<EventTask, String> statusCol = new TableColumn<>("Статус");
        statusCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().status().name()));
        TableColumn<EventTask, String> dueCol = new TableColumn<>("Срок");
        dueCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                data.getValue().dueDate() != null ? data.getValue().dueDate().format(dateFormatter) : "—"));
        TableColumn<EventTask, String> assigneeCol = new TableColumn<>("Ответственный");
        assigneeCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().assignee()));

        taskTable.getColumns().addAll(stageCol, titleCol, statusCol, dueCol, assigneeCol);

        Button addTaskButton = new Button("Добавить задачу");
        addTaskButton.setOnAction(event -> openTaskDialog());

        Button moveToProgress = new Button("В работу");
        moveToProgress.setOnAction(event -> updateTaskStatus(taskTable, TaskStatus.IN_PROGRESS));

        Button markDone = new Button("Завершить");
        markDone.setOnAction(event -> updateTaskStatus(taskTable, TaskStatus.DONE));

        HBox buttons = new HBox(8, addTaskButton, moveToProgress, markDone);
        buttons.setAlignment(Pos.CENTER_LEFT);

        VBox box = new VBox(12, caption, buttons, taskTable);
        VBox.setVgrow(taskTable, Priority.ALWAYS);
        box.setPadding(new Insets(12, 0, 0, 0));
        return box;
    }

    private void loadTasksForEvent(long eventId) {
        selectedEventTasks.setAll(context.eventService().tasksForEvent(eventId));
    }

    private void openTaskDialog() {
        if (currentOrganizerEvent == null) {
            return;
        }
        TaskDialog dialog = new TaskDialog();
        dialog.showAndWait().ifPresent(result -> {
            long eventId = currentOrganizerEvent.id();
            context.eventService().addTask(eventId, result.stage(), result.title(), result.status(), result.dueDate(), result.assignee(), result.notes());
            loadTasksForEvent(eventId);
        });
    }

    private void updateTaskStatus(TableView<EventTask> table, TaskStatus status) {
        EventTask selected = table.getSelectionModel().getSelectedItem();
        if (selected != null) {
            context.eventService().moveTask(selected.id(), status);
            loadTasksForEvent(selected.eventId());
        }
    }

    private void openEventDialog() {
        EventFormDialog dialog = new EventFormDialog(context.referenceDataService().cities(), context.referenceDataService().directions());
        dialog.showAndWait().ifPresent(result -> {
            context.eventService().createEvent(result.title(), result.description(), result.startDate(), result.endDate(),
                    result.cityId(), result.directionId(), user.id(), result.capacity());
            refreshOrganizerData();
        });
    }

    private Tab buildOrganizerModerationTab() {
        Tab tab = new Tab("Запросы модерации");
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(16));

        TableView<ModerationRequest> table = createModerationTable();
        table.setItems(moderationRequests);

        Button refreshButton = new Button("Обновить");
        refreshButton.setOnAction(event -> refreshOrganizerData());

        root.setTop(new HBox(8, refreshButton));
        root.setCenter(table);

        tab.setContent(root);
        return tab;
    }

    private TableView<ModerationRequest> createModerationTable() {
        TableView<ModerationRequest> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        TableColumn<ModerationRequest, Number> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().id()));
        TableColumn<ModerationRequest, Number> eventCol = new TableColumn<>("Мероприятие");
        eventCol.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().eventId()));
        TableColumn<ModerationRequest, String> statusCol = new TableColumn<>("Статус");
        statusCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().status().name()));
        TableColumn<ModerationRequest, String> createdCol = new TableColumn<>("Создано");
        createdCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().createdAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))));
        TableColumn<ModerationRequest, String> messageCol = new TableColumn<>("Сообщение");
        messageCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().message()));
        table.getColumns().addAll(idCol, eventCol, statusCol, createdCol, messageCol);
        return table;
    }

    private Tab buildModeratorRequestsTab() {
        Tab tab = new Tab("Заявки модерации");
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(16));

        TableView<ModerationRequest> table = createModerationTable();
        ObservableList<ModerationRequest> moderatorRequests = FXCollections.observableArrayList();
        table.setItems(moderatorRequests);

        Button refreshButton = new Button("Обновить");
        refreshButton.setOnAction(event -> moderatorRequests.setAll(context.moderationService().pendingRequests()));

        Button approveButton = new Button("Утвердить");
        approveButton.setOnAction(event -> {
            ModerationRequest selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                context.moderationService().approve(selected.id(), selected.eventId(), "Одобрено модератором");
                moderatorRequests.setAll(context.moderationService().pendingRequests());
                refreshCatalog();
            }
        });

        Button rejectButton = new Button("Отклонить");
        rejectButton.setOnAction(event -> {
            ModerationRequest selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                context.moderationService().reject(selected.id(), selected.eventId(), "Требуется доработка");
                moderatorRequests.setAll(context.moderationService().pendingRequests());
            }
        });

        HBox controls = new HBox(8, refreshButton, approveButton, rejectButton);
        controls.setAlignment(Pos.CENTER_LEFT);
        root.setTop(controls);
        root.setCenter(table);

        tab.setContent(root);
        moderatorRequests.setAll(context.moderationService().pendingRequests());
        return tab;
    }

    private Tab buildTaskBoardTab() {
        Tab tab = new Tab("Доска задач");
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(16));

        ListView<String> todoList = new ListView<>();
        ListView<String> inProgressList = new ListView<>();
        ListView<String> doneList = new ListView<>();

        root.setCenter(new HBox(16,
                createTaskColumn("TODO", todoList),
                createTaskColumn("IN_PROGRESS", inProgressList),
                createTaskColumn("DONE", doneList)));

        Button refresh = new Button("Обновить");
        refresh.setOnAction(event -> {
            var events = context.eventService().allEvents();
            todoList.getItems().setAll(events.stream().flatMap(event -> context.eventService().tasksForEvent(event.id()).stream())
                    .filter(task -> task.status() == TaskStatus.TODO)
                    .sorted(Comparator.comparing(EventTask::dueDate, Comparator.nullsLast(Comparator.naturalOrder())))
                    .map(task -> task.title() + " («" + task.stage() + "»)")
                    .collect(Collectors.toList()));
            inProgressList.getItems().setAll(events.stream().flatMap(event -> context.eventService().tasksForEvent(event.id()).stream())
                    .filter(task -> task.status() == TaskStatus.IN_PROGRESS)
                    .map(task -> task.title() + " («" + task.stage() + "»)")
                    .collect(Collectors.toList()));
            doneList.getItems().setAll(events.stream().flatMap(event -> context.eventService().tasksForEvent(event.id()).stream())
                    .filter(task -> task.status() == TaskStatus.DONE)
                    .map(task -> task.title() + " («" + task.stage() + "»)")
                    .collect(Collectors.toList()));
        });

        root.setTop(new HBox(8, refresh));
        tab.setContent(root);
        refresh.fire();
        return tab;
    }

    private VBox createTaskColumn(String title, ListView<String> listView) {
        Label label = new Label(title);
        label.setFont(Font.font("Comic Sans MS", FontWeight.BOLD, 16));
        VBox box = new VBox(8, label, listView);
        VBox.setVgrow(listView, Priority.ALWAYS);
        return box;
    }

    private Tab buildParticipantsDirectoryTab() {
        Tab tab = new Tab("Участники");
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(16));

        TableView<ParticipantProfile> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        TableColumn<ParticipantProfile, Number> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().id()));
        TableColumn<ParticipantProfile, String> nameCol = new TableColumn<>("ФИО");
        nameCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().fullName()));
        TableColumn<ParticipantProfile, String> companyCol = new TableColumn<>("Компания");
        companyCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().company()));
        TableColumn<ParticipantProfile, String> roleCol = new TableColumn<>("Должность");
        roleCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().jobTitle()));
        TableColumn<ParticipantProfile, String> emailCol = new TableColumn<>("Email");
        emailCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().email()));
        TableColumn<ParticipantProfile, String> phoneCol = new TableColumn<>("Телефон");
        phoneCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().phone()));
        table.getColumns().addAll(idCol, nameCol, companyCol, roleCol, emailCol, phoneCol);

        Button refresh = new Button("Обновить");
        refresh.setOnAction(event -> table.setItems(FXCollections.observableArrayList(context.directoryService().participants())));

        root.setTop(new HBox(8, refresh));
        root.setCenter(table);
        tab.setContent(root);
        refresh.fire();
        return tab;
    }

    private void refreshCatalog() {
        catalogEvents.setAll(context.eventService().catalog());
    }

    private void refreshOrganizerData() {
        organizerEvents.setAll(context.eventService().organizerEvents(user.id()));
        currentOrganizerEvent = organizerEvents.isEmpty() ? null : organizerEvents.get(0);
        moderationRequests.setAll(context.moderationService().requestsForOrganizer(user.id()));
        if (currentOrganizerEvent != null) {
            loadTasksForEvent(currentOrganizerEvent.id());
        } else {
            selectedEventTasks.clear();
        }
    }

    private void refreshModeratorData() {
        // currently handled via refresh buttons
    }
}
