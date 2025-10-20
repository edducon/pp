package com.example.eventapp.ui.components;

import com.example.eventapp.domain.City;
import com.example.eventapp.domain.Direction;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import java.time.LocalDate;
import java.util.List;

public class EventFormDialog extends Dialog<EventFormDialog.Result> {
    private final TextField titleField = new TextField();
    private final TextArea descriptionField = new TextArea();
    private final DatePicker startDatePicker = new DatePicker();
    private final DatePicker endDatePicker = new DatePicker();
    private final ComboBox<City> cityComboBox;
    private final ComboBox<Direction> directionComboBox;
    private final Spinner<Integer> capacitySpinner = new Spinner<>();

    public EventFormDialog(List<City> cities, List<Direction> directions) {
        setTitle("Новое мероприятие");
        getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

        descriptionField.setPrefRowCount(4);

        cityComboBox = new ComboBox<>(FXCollections.observableArrayList(cities));
        directionComboBox = new ComboBox<>(FXCollections.observableArrayList(directions));
        capacitySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(10, 5000, 100));

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(16));

        grid.addRow(0, new Label("Название:"), titleField);
        grid.addRow(1, new Label("Описание:"), descriptionField);
        grid.addRow(2, new Label("Дата начала:"), startDatePicker);
        grid.addRow(3, new Label("Дата окончания:"), endDatePicker);
        grid.addRow(4, new Label("Город:"), cityComboBox);
        grid.addRow(5, new Label("Направление:"), directionComboBox);
        grid.addRow(6, new Label("Вместимость:"), capacitySpinner);

        getDialogPane().setContent(grid);
        setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                City city = cityComboBox.getValue();
                Direction direction = directionComboBox.getValue();
                if (titleField.getText().isBlank() || city == null || direction == null) {
                    return null;
                }
                LocalDate start = startDatePicker.getValue();
                LocalDate end = endDatePicker.getValue();
                if (start == null || end == null || end.isBefore(start)) {
                    return null;
                }
                return new Result(titleField.getText().trim(), descriptionField.getText().trim(), start, end,
                        city.id(), direction.id(), capacitySpinner.getValue());
            }
            return null;
        });
    }

    public record Result(String title,
                         String description,
                         LocalDate startDate,
                         LocalDate endDate,
                         long cityId,
                         long directionId,
                         int capacity) {
    }
}
