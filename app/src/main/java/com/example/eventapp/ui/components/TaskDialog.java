package com.example.eventapp.ui.components;

import com.example.eventapp.domain.TaskStatus;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import java.time.LocalDate;

public class TaskDialog extends Dialog<TaskDialog.TaskResult> {
    private final TextField stageField = new TextField();
    private final TextField titleField = new TextField();
    private final ComboBox<TaskStatus> statusComboBox = new ComboBox<>(FXCollections.observableArrayList(TaskStatus.values()));
    private final DatePicker dueDatePicker = new DatePicker();
    private final TextField assigneeField = new TextField();
    private final TextArea notesField = new TextArea();

    public TaskDialog() {
        setTitle("Новая задача мероприятия");
        getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        notesField.setPrefRowCount(4);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(16));

        grid.addRow(0, new Label("Этап:"), stageField);
        grid.addRow(1, new Label("Название:"), titleField);
        grid.addRow(2, new Label("Статус:"), statusComboBox);
        grid.addRow(3, new Label("Срок:"), dueDatePicker);
        grid.addRow(4, new Label("Ответственный:"), assigneeField);
        grid.addRow(5, new Label("Заметки:"), notesField);

        statusComboBox.getSelectionModel().select(TaskStatus.TODO);

        getDialogPane().setContent(grid);
        setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                if (stageField.getText().isBlank() || titleField.getText().isBlank()) {
                    return null;
                }
                LocalDate due = dueDatePicker.getValue();
                return new TaskResult(stageField.getText().trim(), titleField.getText().trim(),
                        statusComboBox.getValue(), due, assigneeField.getText().trim(), notesField.getText().trim());
            }
            return null;
        });
    }

    public record TaskResult(String stage,
                             String title,
                             TaskStatus status,
                             LocalDate dueDate,
                             String assignee,
                             String notes) {
    }
}
