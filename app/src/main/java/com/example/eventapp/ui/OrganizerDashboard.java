package com.example.eventapp.ui;

import com.example.eventapp.model.AuthenticatedUser;
import com.example.eventapp.model.Event;
import com.example.eventapp.model.FullName;
import com.example.eventapp.model.Gender;
import com.example.eventapp.model.Organizer;
import com.example.eventapp.service.AppContext;
import com.example.eventapp.util.CsvExporter;
import com.example.eventapp.util.PasswordHasher;
import com.example.eventapp.util.TimeOfDayGreeting;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Date;

public class OrganizerDashboard extends JFrame {
    private final AppContext context;
    private final AuthenticatedUser user;
    private Organizer organizer;

    private final JTextField lastNameField = new JTextField(20);
    private final JTextField firstNameField = new JTextField(20);
    private final JTextField middleNameField = new JTextField(20);
    private final JTextField emailField = new JTextField(25);
    private final JTextField phoneField = new JTextField(16);
    private final JTextField countryField = new JTextField(4);
    private final JComboBox<String> genderBox = new JComboBox<>(new String[]{Gender.MALE.getDisplayName(), Gender.FEMALE.getDisplayName()});
    private final JComboBox<String> cityBox = new JComboBox<>();
    private final JTextField photoField = new JTextField(25);
    private final JPasswordField passwordField = new JPasswordField(16);
    private final JPasswordField confirmPasswordField = new JPasswordField(16);

    private final DefaultTableModel eventsModel = new DefaultTableModel(new Object[]{"Название", "Направление", "Дата", "Город", "Заявки"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable eventsTable = new JTable(eventsModel);

    public OrganizerDashboard(Frame owner, AppContext context, AuthenticatedUser user) {
        super("Окно организатора");
        this.context = context;
        this.user = user;
        this.organizer = (Organizer) context.authenticationService().reloadAccount(user);
        setSize(1000, 720);
        setLocationRelativeTo(owner);
        initUi();
        loadProfile();
        loadEvents();
    }

    private void initUi() {
        setLayout(new BorderLayout());
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        JLabel greetingLabel = new JLabel();
        greetingLabel.setBorder(new EmptyBorder(10, 10, 10, 10));
        greetingLabel.setFont(greetingLabel.getFont().deriveFont(Font.BOLD, 16f));
        add(greetingLabel, BorderLayout.NORTH);
        updateGreeting(greetingLabel);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Профиль", createProfilePanel());
        tabs.addTab("Мероприятия", createEventsPanel());
        tabs.addTab("Справочники", createDirectoriesPanel());
        add(tabs, BorderLayout.CENTER);
    }

    private void updateGreeting(JLabel label) {
        String greeting = TimeOfDayGreeting.forTime(java.time.LocalTime.now());
        label.setText("%s, %s!".formatted(greeting, organizer.getFullName().forGreeting()));
    }

    private JPanel createProfilePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel photoLabel = new JLabel(loadPhotoIcon(organizer.getPhotoPath()));
        panel.add(photoLabel, BorderLayout.WEST);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;
        int row = 0;
        gbc.gridx = 0;
        gbc.gridy = row;
        form.add(new JLabel("Фамилия:"), gbc);
        gbc.gridx = 1;
        form.add(lastNameField, gbc);

        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        form.add(new JLabel("Имя:"), gbc);
        gbc.gridx = 1;
        form.add(firstNameField, gbc);

        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        form.add(new JLabel("Отчество:"), gbc);
        gbc.gridx = 1;
        form.add(middleNameField, gbc);

        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        form.add(new JLabel("Email:"), gbc);
        gbc.gridx = 1;
        form.add(emailField, gbc);

        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        form.add(new JLabel("Телефон:"), gbc);
        gbc.gridx = 1;
        form.add(phoneField, gbc);

        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        form.add(new JLabel("Страна:"), gbc);
        gbc.gridx = 1;
        form.add(countryField, gbc);

        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        form.add(new JLabel("Город:"), gbc);
        gbc.gridx = 1;
        cityBox.setEditable(true);
        context.cityDao().findAll().forEach(city -> cityBox.addItem(city.getName()));
        form.add(cityBox, gbc);

        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        form.add(new JLabel("Пол:"), gbc);
        gbc.gridx = 1;
        form.add(genderBox, gbc);

        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        form.add(new JLabel("Фото:"), gbc);
        gbc.gridx = 1;
        form.add(photoField, gbc);

        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        form.add(new JLabel("Пароль:"), gbc);
        gbc.gridx = 1;
        form.add(passwordField, gbc);

        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        form.add(new JLabel("Повторите пароль:"), gbc);
        gbc.gridx = 1;
        form.add(confirmPasswordField, gbc);

        JButton showPassword = new JButton("Показать");
        showPassword.addActionListener(e -> {
            boolean echo = passwordField.getEchoChar() != 0;
            passwordField.setEchoChar(echo ? (char) 0 : '*');
            confirmPasswordField.setEchoChar(echo ? (char) 0 : '*');
            showPassword.setText(echo ? "Скрыть" : "Показать");
        });
        gbc.gridx = 2;
        gbc.gridy = row;
        form.add(showPassword, gbc);

        JButton saveButton = new JButton("Сохранить");
        saveButton.addActionListener(e -> saveProfile());
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(saveButton);

        panel.add(form, BorderLayout.CENTER);
        panel.add(bottom, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createEventsPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextField directionField = new JTextField(12);
        JSpinner dateSpinner = new JSpinner(new SpinnerDateModel());
        dateSpinner.setEditor(new JSpinner.DateEditor(dateSpinner, "dd.MM.yyyy"));
        JCheckBox useDateFilter = new JCheckBox("Использовать дату");
        filters.add(new JLabel("Направление:"));
        filters.add(directionField);
        filters.add(new JLabel("Дата:"));
        filters.add(dateSpinner);
        filters.add(useDateFilter);
        JButton filterButton = new JButton("Применить");
        filterButton.addActionListener(e -> {
            LocalDate date = null;
            if (useDateFilter.isSelected()) {
                Date value = (Date) dateSpinner.getValue();
                date = value.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            }
            applyEventFilters(directionField.getText(), date);
        });
        JButton resetButton = new JButton("Сбросить");
        resetButton.addActionListener(e -> {
            directionField.setText("");
            useDateFilter.setSelected(false);
            loadEvents();
        });
        filters.add(filterButton);
        filters.add(resetButton);

        eventsTable.setRowHeight(26);
        JScrollPane scrollPane = new JScrollPane(eventsTable);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton createButton = new JButton("Создать мероприятие");
        createButton.addActionListener(e -> openCreationDialog());
        JButton csvButton = new JButton("Экспорт в CSV");
        csvButton.addActionListener(e -> exportSelectedEvent());
        JButton kanbanButton = new JButton("Kanban-доска");
        kanbanButton.addActionListener(e -> openKanban());
        JButton requestsButton = new JButton("Заявки модераторов");
        requestsButton.addActionListener(e -> openModerationRequests());
        actions.add(requestsButton);
        actions.add(kanbanButton);
        actions.add(csvButton);
        actions.add(createButton);

        panel.add(filters, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(actions, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createDirectoriesPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton juryButton = new JButton("Жюри и модераторы");
        juryButton.addActionListener(e -> new JuryModeratorDirectoryFrame(this, context).setVisible(true));
        JButton participantsButton = new JButton("Участники");
        participantsButton.addActionListener(e -> new ParticipantDirectoryFrame(this, context, Optional.of(user)).setVisible(true));
        panel.add(juryButton);
        panel.add(participantsButton);
        return panel;
    }

    private void loadProfile() {
        lastNameField.setText(organizer.getFullName().lastName());
        firstNameField.setText(organizer.getFullName().firstName());
        middleNameField.setText(organizer.getFullName().middleName());
        emailField.setText(organizer.getEmail());
        phoneField.setText(organizer.getPhone());
        countryField.setText(organizer.getCountryCode());
        cityBox.setSelectedItem(context.cityDao().findById(organizer.getCityId()).map(city -> city.getName()).orElse(""));
        genderBox.setSelectedItem(organizer.getGender().getDisplayName());
        photoField.setText(organizer.getPhotoPath());
    }

    private void saveProfile() {
        if (!String.valueOf(passwordField.getPassword()).equals(String.valueOf(confirmPasswordField.getPassword()))) {
            JOptionPane.showMessageDialog(this, "Пароли не совпадают", "Ошибка", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String cityName = (String) cityBox.getSelectedItem();
        if (cityName == null || cityName.isBlank()) {
            JOptionPane.showMessageDialog(this, "Укажите город", "Ошибка", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int cityId = context.cityDao().findByName(cityName).orElseGet(() -> context.cityDao().insertCity(cityName, organizer.getCountryCode())).getId();
        Organizer updated = new Organizer(organizer.getId(), organizer.getIdNumber(),
                new FullName(lastNameField.getText(), firstNameField.getText(), middleNameField.getText()),
                emailField.getText(), organizer.getBirthDate(), countryField.getText(), cityId, phoneField.getText(),
                genderBox.getSelectedItem().equals(Gender.MALE.getDisplayName()) ? Gender.MALE : Gender.FEMALE, photoField.getText());
        String password = new String(passwordField.getPassword());
        context.organizerDao().updateProfile(updated, password.isBlank() ? null : PasswordHasher.hash(password));
        organizer = updated;
        JOptionPane.showMessageDialog(this, "Профиль сохранен", "Готово", JOptionPane.INFORMATION_MESSAGE);
    }

    private void loadEvents() {
        List<Event> events = context.eventService().loadOrganizerEvents(organizer.getId());
        eventsModel.setRowCount(0);
        var pending = context.eventService().findEventsWithPendingRequests(organizer.getId());
        for (Event event : events) {
            String city = context.cityDao().findById(event.getCityId()).map(c -> c.getName()).orElse("-");
            boolean hasRequests = pending.contains(event.getId());
            eventsModel.addRow(new Object[]{event.getTitle(), event.getDirection().getName(), event.getStartTime().toLocalDate(), city, hasRequests ? "Есть" : "-"});
        }
    }

    private void applyEventFilters(String directionFilter, LocalDate date) {
        List<Event> events = context.eventService().loadOrganizerEvents(organizer.getId());
        eventsModel.setRowCount(0);
        var pending = context.eventService().findEventsWithPendingRequests(organizer.getId());
        for (Event event : events) {
            if (directionFilter != null && !directionFilter.isBlank() && !event.getDirection().getName().toLowerCase().contains(directionFilter.toLowerCase())) {
                continue;
            }
            if (date != null && !event.getStartTime().toLocalDate().equals(date)) {
                continue;
            }
            String city = context.cityDao().findById(event.getCityId()).map(c -> c.getName()).orElse("-");
            boolean hasRequests = pending.contains(event.getId());
            eventsModel.addRow(new Object[]{event.getTitle(), event.getDirection().getName(), event.getStartTime().toLocalDate(), city, hasRequests ? "Есть" : "-"});
        }
    }

    private void openCreationDialog() {
        EventCreationDialog dialog = new EventCreationDialog(this, context, organizer);
        dialog.setVisible(true);
        if (dialog.isCreated()) {
            loadEvents();
        }
    }

    private void exportSelectedEvent() {
        int row = eventsTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Выберите мероприятие", "Ошибка", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String title = (String) eventsModel.getValueAt(row, 0);
        context.eventService().loadOrganizerEvents(organizer.getId()).stream()
                .filter(e -> e.getTitle().equals(title))
                .findFirst()
                .flatMap(event -> context.eventService().loadEvent(event.getId()))
                .ifPresentOrElse(event -> {
                    JFileChooser chooser = new JFileChooser();
                    chooser.setSelectedFile(new File("event.csv"));
                    if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                        try (FileWriter writer = new FileWriter(chooser.getSelectedFile())) {
                            CsvExporter.exportEvent(event, writer);
                            JOptionPane.showMessageDialog(this, "Файл сохранен", "Готово", JOptionPane.INFORMATION_MESSAGE);
                        } catch (IOException ex) {
                            JOptionPane.showMessageDialog(this, "Ошибка экспорта: " + ex.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }, () -> JOptionPane.showMessageDialog(this, "Не удалось загрузить данные", "Ошибка", JOptionPane.ERROR_MESSAGE));
    }

    private void openKanban() {
        int row = eventsTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Выберите мероприятие", "Ошибка", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String title = (String) eventsModel.getValueAt(row, 0);
        context.eventService().loadOrganizerEvents(organizer.getId()).stream()
                .filter(e -> e.getTitle().equals(title))
                .findFirst()
                .flatMap(event -> context.eventService().loadEvent(event.getId()))
                .ifPresent(event -> new KanbanBoardFrame(this, context, event).setVisible(true));
    }

    private void openModerationRequests() {
        int row = eventsTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Выберите мероприятие", "Ошибка", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String title = (String) eventsModel.getValueAt(row, 0);
        context.eventService().loadOrganizerEvents(organizer.getId()).stream()
                .filter(e -> e.getTitle().equals(title))
                .findFirst()
                .flatMap(event -> context.eventService().loadEvent(event.getId()))
                .ifPresent(event -> new ModerationRequestsDialog(this, context, event).setVisible(true));
    }

    private Icon loadPhotoIcon(String path) {
        if (path == null || path.isBlank()) {
            return UIManager.getIcon("FileView.fileIcon");
        }
        try {
            Image image = new ImageIcon(path).getImage().getScaledInstance(120, 120, Image.SCALE_SMOOTH);
            return new ImageIcon(image);
        } catch (Exception ex) {
            return UIManager.getIcon("FileView.fileIcon");
        }
    }
}
