package com.example.eventapp.ui;

import com.example.eventapp.model.AuthenticatedUser;
import com.example.eventapp.model.Event;
import com.example.eventapp.model.Role;
import com.example.eventapp.service.AppContext;
import com.example.eventapp.util.TimeOfDayGreeting;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class MainWindow extends JFrame {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final AppContext context;
    private final DefaultTableModel tableModel;
    private final JTable eventsTable;
    private final JComboBox<String> directionFilter;
    private final JSpinner dateSpinner;
    private List<Event> currentEvents = List.of();

    public MainWindow(AppContext context) {
        super("Платформа управления мероприятиями");
        this.context = context;
        this.tableModel = new DefaultTableModel(new Object[]{"Мероприятие", "Направление", "Дата", "Город"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        this.eventsTable = new JTable(tableModel);
        this.directionFilter = new JComboBox<>();
        this.dateSpinner = createDateSpinner();
        initUi();
        loadEvents(null, null);
    }

    private void initUi() {
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(1100, 720);
        setLocationRelativeTo(null);

        JPanel filterPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        filterPanel.add(new JLabel("Направление:"), gbc);

        directionFilter.setEditable(true);
        directionFilter.addItem("");
        context.directionDao().findAll().forEach(direction -> directionFilter.addItem(direction.getName()));
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        filterPanel.add(directionFilter, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        filterPanel.add(new JLabel("Дата:"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        filterPanel.add(dateSpinner, gbc);

        JButton applyFilters = new JButton("Применить");
        applyFilters.addActionListener(e -> applyFilters());
        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.gridheight = 2;
        gbc.fill = GridBagConstraints.VERTICAL;
        filterPanel.add(applyFilters, gbc);

        JButton resetFilters = new JButton("Сбросить");
        resetFilters.addActionListener(e -> {
            directionFilter.setSelectedItem("");
            dateSpinner.setValue(null);
            loadEvents(null, null);
        });
        gbc.gridx = 3;
        gbc.gridheight = 2;
        filterPanel.add(resetFilters, gbc);

        eventsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        eventsTable.setRowHeight(28);
        JScrollPane tableScroll = new JScrollPane(eventsTable);

        JButton detailsButton = new JButton("Подробная информация");
        detailsButton.addActionListener(e -> openDetails());

        JButton loginButton = new JButton("Авторизация");
        loginButton.addActionListener(e -> openLogin());

        JButton registerParticipant = new JButton("Регистрация участника");
        registerParticipant.addActionListener(e -> ParticipantRegistrationDialog.open(this, context));

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.add(registerParticipant);
        bottomPanel.add(detailsButton);
        bottomPanel.add(loginButton);

        add(filterPanel, BorderLayout.NORTH);
        add(tableScroll, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void applyFilters() {
        String direction = Optional.ofNullable(directionFilter.getSelectedItem()).map(Object::toString).filter(s -> !s.isBlank()).orElse(null);
        Object dateValue = dateSpinner.getValue();
        LocalDate date = null;
        if (dateValue instanceof java.util.Date utilDate) {
            date = new java.sql.Date(utilDate.getTime()).toLocalDate();
        }
        loadEvents(direction, date);
    }

    private void loadEvents(String direction, LocalDate date) {
        List<Event> events = context.eventService().loadMainEvents(direction, date);
        currentEvents = events;
        tableModel.setRowCount(0);
        for (Event event : events) {
            String dateValue = event.getStartTime().toLocalDate().format(DATE_FORMATTER);
            String cityName = context.cityDao().findById(event.getCityId()).map(city -> city.getName()).orElse("-");
            tableModel.addRow(new Object[]{event.getTitle(), event.getDirection().getName(), dateValue, cityName});
        }
        if (tableModel.getRowCount() > 0) {
            eventsTable.getSelectionModel().setSelectionInterval(0, 0);
        }
    }

    private void openDetails() {
        int selectedRow = eventsTable.getSelectedRow();
        if (selectedRow < 0 || selectedRow >= tableModel.getRowCount()) {
            JOptionPane.showMessageDialog(this, "Выберите мероприятие", "Внимание", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (selectedRow >= currentEvents.size()) {
            JOptionPane.showMessageDialog(this, "Не удалось определить мероприятие", "Ошибка", JOptionPane.ERROR_MESSAGE);
            return;
        }
        Event summary = currentEvents.get(selectedRow);
        context.eventService().loadEvent(summary.getId())
                .ifPresentOrElse(event -> new EventDetailsDialog(this, context, event).setVisible(true),
                        () -> JOptionPane.showMessageDialog(this, "Не удалось загрузить мероприятие", "Ошибка", JOptionPane.ERROR_MESSAGE));
    }

    private void openLogin() {
        LoginDialog dialog = new LoginDialog(this, context);
        dialog.setVisible(true);
        dialog.getAuthenticatedUser().ifPresent(this::openRoleWindow);
    }

    private void openRoleWindow(AuthenticatedUser user) {
        switch (user.getRole()) {
            case ORGANIZER -> new OrganizerDashboard(this, context, user).setVisible(true);
            case MODERATOR -> new ModeratorDashboard(this, context, user).setVisible(true);
            case JURY -> JOptionPane.showMessageDialog(this,
                    "Добро пожаловать, %s (%s)!".formatted(user.getAccount().getFullName().forGreeting(), TimeOfDayGreeting.forTime(java.time.LocalTime.now())),
                    "Профиль жюри", JOptionPane.INFORMATION_MESSAGE);
            case PARTICIPANT -> new ParticipantDirectoryFrame(this, context, Optional.of(user)).setVisible(true);
        }
    }

    private JSpinner createDateSpinner() {
        SpinnerDateModel model = new SpinnerDateModel();
        model.setCalendarField(java.util.Calendar.DAY_OF_MONTH);
        JSpinner spinner = new JSpinner(model);
        spinner.setEditor(new JSpinner.DateEditor(spinner, "dd.MM.yyyy"));
        spinner.setValue(null);
        return spinner;
    }
}
