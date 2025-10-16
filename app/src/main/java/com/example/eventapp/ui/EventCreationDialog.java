package com.example.eventapp.ui;

import com.example.eventapp.model.Event;
import com.example.eventapp.model.EventActivity;
import com.example.eventapp.model.JuryMember;
import com.example.eventapp.model.Organizer;
import com.example.eventapp.service.AppContext;
import com.example.eventapp.service.EventService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Date;

public class EventCreationDialog extends JDialog {
    private final AppContext context;
    private final Organizer organizer;

    private final JComboBox<String> titleField = new JComboBox<>();
    private final JComboBox<String> directionField = new JComboBox<>();
    private final JComboBox<String> cityField = new JComboBox<>();
    private final JTextField logoField = new JTextField(25);
    private final JTextArea descriptionArea = new JTextArea(4, 30);
    private final JSpinner startDateSpinner = new JSpinner(new SpinnerDateModel());
    private final JSpinner startTimeSpinner = new JSpinner(new SpinnerDateModel());
    private final JSpinner endTimeSpinner = new JSpinner(new SpinnerDateModel());
    private final DefaultListModel<EventActivity> activitiesModel = new DefaultListModel<>();
    private final JList<EventActivity> activitiesList = new JList<>(activitiesModel);
    private final Map<Long, List<String>> activityJury = new HashMap<>();
    private long activitySequence = -1;
    private boolean created;

    public EventCreationDialog(Frame owner, AppContext context, Organizer organizer) {
        super(owner, "Создание мероприятия", true);
        this.context = context;
        this.organizer = organizer;
        setSize(720, 600);
        setLocationRelativeTo(owner);
        initUi();
    }

    private void initUi() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Название:"), gbc);
        gbc.gridx = 1;
        titleField.setEditable(true);
        panel.add(titleField, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        panel.add(new JLabel("Направление:"), gbc);
        gbc.gridx = 1;
        directionField.setEditable(true);
        context.eventService().suggestDirections("").forEach(directionField::addItem);
        panel.add(directionField, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        panel.add(new JLabel("Город:"), gbc);
        gbc.gridx = 1;
        cityField.setEditable(true);
        context.cityDao().findAll().forEach(city -> cityField.addItem(city.getName()));
        panel.add(cityField, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        panel.add(new JLabel("Логотип (путь):"), gbc);
        gbc.gridx = 1;
        panel.add(logoField, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        panel.add(new JLabel("Описание:"), gbc);
        gbc.gridx = 1;
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        panel.add(new JScrollPane(descriptionArea), gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        panel.add(new JLabel("Дата начала:"), gbc);
        gbc.gridx = 1;
        startDateSpinner.setEditor(new JSpinner.DateEditor(startDateSpinner, "dd.MM.yyyy"));
        panel.add(startDateSpinner, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        panel.add(new JLabel("Время начала:"), gbc);
        gbc.gridx = 1;
        startTimeSpinner.setEditor(new JSpinner.DateEditor(startTimeSpinner, "HH:mm"));
        panel.add(startTimeSpinner, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        panel.add(new JLabel("Время окончания:"), gbc);
        gbc.gridx = 1;
        endTimeSpinner.setEditor(new JSpinner.DateEditor(endTimeSpinner, "HH:mm"));
        panel.add(endTimeSpinner, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        panel.add(new JLabel("Активности:"), gbc);
        gbc.gridy++;
        activitiesList.setVisibleRowCount(6);
        panel.add(new JScrollPane(activitiesList), gbc);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton addActivity = new JButton("Добавить активность");
        addActivity.addActionListener(e -> openActivityDialog());
        JButton removeActivity = new JButton("Удалить");
        removeActivity.addActionListener(e -> removeSelectedActivity());
        buttons.add(removeActivity);
        buttons.add(addActivity);

        JButton saveButton = new JButton("Создать");
        saveButton.addActionListener(e -> saveEvent());
        JButton cancelButton = new JButton("Отмена");
        cancelButton.addActionListener(e -> dispose());

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actionPanel.add(cancelButton);
        actionPanel.add(saveButton);

        add(panel, BorderLayout.CENTER);
        add(buttons, BorderLayout.NORTH);
        add(actionPanel, BorderLayout.SOUTH);
    }

    private void openActivityDialog() {
        LocalDate date = ((java.util.Date) startDateSpinner.getValue()).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalTime startTime = ((java.util.Date) startTimeSpinner.getValue()).toInstant().atZone(ZoneId.systemDefault()).toLocalTime();
        LocalTime endTime = ((java.util.Date) endTimeSpinner.getValue()).toInstant().atZone(ZoneId.systemDefault()).toLocalTime();
        LocalDateTime eventStart = LocalDateTime.of(date, startTime);
        LocalDateTime eventEnd = LocalDateTime.of(date, endTime);
        List<EventActivity> scheduled = new ArrayList<>();
        for (int i = 0; i < activitiesModel.getSize(); i++) {
            scheduled.add(activitiesModel.get(i));
        }
        List<EventService.TimeSlot> slots = context.eventService().calculateAvailableSlots(eventStart, eventEnd, scheduled);
        if (slots.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Свободных временных интервалов нет", "Информация", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        ActivityEditorDialog dialog = new ActivityEditorDialog(this, slots, context);
        dialog.setVisible(true);
        if (dialog.isApproved()) {
            EventService.TimeSlot slot = dialog.getSelectedSlot();
            EventActivity activity = new EventActivity(activitySequence--, 0,
                    dialog.getTitleValue(), dialog.getDescriptionValue(), slot.start(), slot.end());
            activityJury.put(activity.getId(), dialog.getSelectedJuryIds());
            activitiesModel.addElement(activity);
        }
    }

    private void removeSelectedActivity() {
        int index = activitiesList.getSelectedIndex();
        if (index >= 0) {
            EventActivity activity = activitiesModel.remove(index);
            activityJury.remove(activity.getId());
        }
    }

    private void saveEvent() {
        if (titleField.getEditor().getItem().toString().isBlank()) {
            JOptionPane.showMessageDialog(this, "Введите название мероприятия", "Ошибка", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (activitiesModel.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Добавьте хотя бы одну активность", "Ошибка", JOptionPane.ERROR_MESSAGE);
            return;
        }
        LocalDate date = ((java.util.Date) startDateSpinner.getValue()).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalTime startTime = ((java.util.Date) startTimeSpinner.getValue()).toInstant().atZone(ZoneId.systemDefault()).toLocalTime();
        LocalTime endTime = ((java.util.Date) endTimeSpinner.getValue()).toInstant().atZone(ZoneId.systemDefault()).toLocalTime();
        if (!endTime.isAfter(startTime)) {
            JOptionPane.showMessageDialog(this, "Время окончания должно быть позже начала", "Ошибка", JOptionPane.ERROR_MESSAGE);
            return;
        }
        LocalDateTime eventStart = LocalDateTime.of(date, startTime);
        LocalDateTime eventEnd = LocalDateTime.of(date, endTime);
        if (eventEnd.isAfter(eventStart.plusHours(24))) {
            JOptionPane.showMessageDialog(this, "Продолжительность мероприятия не может превышать сутки", "Ошибка", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String directionName = directionField.getEditor().getItem().toString().trim();
        if (directionName.isBlank()) {
            JOptionPane.showMessageDialog(this, "Укажите направление", "Ошибка", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String cityName = cityField.getEditor().getItem().toString().trim();
        if (cityName.isBlank()) {
            JOptionPane.showMessageDialog(this, "Укажите город", "Ошибка", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String logoPath = logoField.getText().trim();
        Event event = new Event(0, organizer.getId(), titleField.getEditor().getItem().toString().trim(),
                context.eventService().ensureDirection(directionName), descriptionArea.getText(), logoPath,
                context.eventService().ensureCity(cityName, organizer.getCountryCode()).getId(), eventStart, eventEnd);
        List<EventActivity> activities = new ArrayList<>();
        Map<String, List<String>> juryMap = new HashMap<>();
        for (int i = 0; i < activitiesModel.size(); i++) {
            EventActivity activity = activitiesModel.get(i);
            activities.add(activity);
            juryMap.put(String.valueOf(activity.getId()), activityJury.getOrDefault(activity.getId(), List.of()));
        }
        context.eventService().createEvent(event, activities, juryMap);
        created = true;
        dispose();
    }

    public boolean isCreated() {
        return created;
    }

    private static class ActivityEditorDialog extends JDialog {
        private final JComboBox<EventService.TimeSlot> slotBox = new JComboBox<>();
        private final JTextField titleField = new JTextField(20);
        private final JTextArea descriptionArea = new JTextArea(4, 20);
        private final JList<JuryMember> juryList;
        private boolean approved;

        ActivityEditorDialog(Dialog owner, List<EventService.TimeSlot> slots, AppContext context) {
            super(owner, "Активность", true);
            setSize(480, 420);
            setLocationRelativeTo(owner);
            setLayout(new BorderLayout(8, 8));
            JPanel form = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(4, 4, 4, 4);
            gbc.anchor = GridBagConstraints.WEST;
            gbc.gridx = 0;
            gbc.gridy = 0;
            form.add(new JLabel("Название:"), gbc);
            gbc.gridx = 1;
            form.add(titleField, gbc);

            gbc.gridy++;
            gbc.gridx = 0;
            form.add(new JLabel("Описание:"), gbc);
            gbc.gridx = 1;
            descriptionArea.setLineWrap(true);
            descriptionArea.setWrapStyleWord(true);
            form.add(new JScrollPane(descriptionArea), gbc);

            gbc.gridy++;
            gbc.gridx = 0;
            form.add(new JLabel("Временной слот:"), gbc);
            gbc.gridx = 1;
            slots.forEach(slotBox::addItem);
            form.add(slotBox, gbc);

            juryList = new JList<>(context.juryMemberDao().findAll().toArray(new JuryMember[0]));
            juryList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            juryList.setVisibleRowCount(6);
            add(form, BorderLayout.NORTH);
            add(new JScrollPane(juryList), BorderLayout.CENTER);

            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton cancelButton = new JButton("Отмена");
            cancelButton.addActionListener(e -> dispose());
            JButton okButton = new JButton("Добавить");
            okButton.addActionListener(e -> {
                if (titleField.getText().isBlank()) {
                    JOptionPane.showMessageDialog(this, "Укажите название активности", "Ошибка", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                approved = true;
                dispose();
            });
            buttons.add(cancelButton);
            buttons.add(okButton);
            add(buttons, BorderLayout.SOUTH);
        }

        boolean isApproved() {
            return approved;
        }

        EventService.TimeSlot getSelectedSlot() {
            return (EventService.TimeSlot) slotBox.getSelectedItem();
        }

        String getTitleValue() {
            return titleField.getText().trim();
        }

        String getDescriptionValue() {
            return descriptionArea.getText();
        }

        List<String> getSelectedJuryIds() {
            return juryList.getSelectedValuesList().stream().map(j -> j.getIdNumber()).toList();
        }
    }
}
