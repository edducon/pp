package com.example.eventapp.ui;

import com.example.eventapp.model.AuthenticatedUser;
import com.example.eventapp.model.EventActivity;
import com.example.eventapp.model.ModerationRequest;
import com.example.eventapp.model.Moderator;
import com.example.eventapp.service.AppContext;
import com.example.eventapp.util.TimeOfDayGreeting;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ModeratorDashboard extends JFrame {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final AppContext context;
    private final AuthenticatedUser user;
    private Moderator moderator;

    private final DefaultTableModel availableModel = new DefaultTableModel(new Object[]{"Мероприятие", "Активность", "Время", "Статус"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable availableTable = new JTable(availableModel);
    private final DefaultTableModel requestsModel = new DefaultTableModel(new Object[]{"Создана", "Активность", "Статус", "Ответ", "Причина"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable requestsTable = new JTable(requestsModel);
    private final DefaultListModel<EventActivity> myActivitiesModel = new DefaultListModel<>();
    private final JList<EventActivity> myActivitiesList = new JList<>(myActivitiesModel);
    private final ActivityTasksPanel tasksPanel;

    private List<EventActivity> allActivities = new ArrayList<>();
    private List<ModerationRequest> myRequests = new ArrayList<>();
    private List<EventActivity> filteredActivities = new ArrayList<>();

    public ModeratorDashboard(Frame owner, AppContext context, AuthenticatedUser user) {
        super("Окно модератора");
        this.context = context;
        this.user = user;
        this.moderator = (Moderator) context.authenticationService().reloadAccount(user);
        this.tasksPanel = new ActivityTasksPanel(context, moderator.getId(), true);
        setSize(1024, 720);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        initUi();
        reloadData();
    }

    private void initUi() {
        setLayout(new BorderLayout());
        JLabel greeting = new JLabel();
        greeting.setBorder(new EmptyBorder(10, 10, 10, 10));
        greeting.setFont(greeting.getFont().deriveFont(Font.BOLD, 16f));
        updateGreeting(greeting);
        add(greeting, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Активности", createActivitiesTab());
        tabs.addTab("Мои заявки", createRequestsTab());
        tabs.addTab("Мои активности", createMyActivitiesTab());
        add(tabs, BorderLayout.CENTER);
    }

    private JPanel createActivitiesTab() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextField searchField = new JTextField(20);
        JCheckBox futureOnly = new JCheckBox("Только предстоящие", true);
        JButton apply = new JButton("Применить");
        apply.addActionListener(e -> updateAvailableTable(searchField.getText(), futureOnly.isSelected()));
        JButton reset = new JButton("Сброс");
        reset.addActionListener(e -> {
            searchField.setText("");
            futureOnly.setSelected(true);
            updateAvailableTable(null, true);
        });
        filters.add(new JLabel("Поиск:"));
        filters.add(searchField);
        filters.add(futureOnly);
        filters.add(apply);
        filters.add(reset);
        panel.add(filters, BorderLayout.NORTH);

        availableTable.setRowHeight(26);
        JScrollPane scrollPane = new JScrollPane(availableTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton refresh = new JButton("Обновить");
        refresh.addActionListener(e -> reloadData());
        JButton requestButton = new JButton("Подать заявку");
        requestButton.addActionListener(e -> submitSelectedRequest());
        JButton manageButton = new JButton("Задачи и ресурсы");
        manageButton.addActionListener(e -> openTasksForSelected());
        actions.add(manageButton);
        actions.add(requestButton);
        actions.add(refresh);
        panel.add(actions, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createRequestsTab() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        requestsTable.setRowHeight(26);
        JScrollPane scrollPane = new JScrollPane(requestsTable);
        panel.add(scrollPane, BorderLayout.CENTER);
        JButton cancel = new JButton("Отменить заявку");
        cancel.addActionListener(e -> cancelSelectedRequest());
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(cancel);
        panel.add(bottom, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createMyActivitiesTab() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        myActivitiesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        myActivitiesList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                if (value instanceof EventActivity activity) {
                    value = "%s (%s)".formatted(activity.getTitle(), activity.getStartTime().format(DATE_TIME_FORMATTER));
                }
                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }
        });
        myActivitiesList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                EventActivity selected = myActivitiesList.getSelectedValue();
                if (selected != null) {
                    loadActivityIntoPanel(selected);
                }
            }
        });
        panel.add(new JScrollPane(myActivitiesList), BorderLayout.WEST);
        panel.add(tasksPanel, BorderLayout.CENTER);
        return panel;
    }

    private void updateGreeting(JLabel label) {
        String greeting = TimeOfDayGreeting.forTime(java.time.LocalTime.now());
        label.setText("%s, %s!".formatted(greeting, moderator.getFullName().forGreeting()));
    }

    private void reloadData() {
        moderator = (Moderator) context.authenticationService().reloadAccount(user);
        allActivities = context.moderatorService().loadAllActivities();
        myRequests = context.moderatorService().loadModeratorRequests(moderator.getId());
        updateAvailableTable(null, true);
        updateRequestsTable();
        updateMyActivities();
    }

    private void updateAvailableTable(String query, boolean futureOnly) {
        Map<Long, ModerationRequest> latestRequestByActivity = collectLatestRequests();
        availableModel.setRowCount(0);
        filteredActivities = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (EventActivity activity : allActivities) {
            if (query != null && !query.isBlank()) {
                String q = query.toLowerCase();
                if (!activity.getTitle().toLowerCase().contains(q) && !activity.getEventTitle().toLowerCase().contains(q)) {
                    continue;
                }
            }
            if (futureOnly && activity.getEndTime().isBefore(now)) {
                continue;
            }
            ModerationRequest latest = latestRequestByActivity.get(activity.getId());
            String status = statusLabel(activity, latest);
            filteredActivities.add(activity);
            availableModel.addRow(new Object[]{activity.getEventTitle(), activity.getTitle(),
                    "%s - %s".formatted(activity.getStartTime().format(DATE_TIME_FORMATTER), activity.getEndTime().format(DATE_TIME_FORMATTER)), status});
        }
    }

    private void updateRequestsTable() {
        requestsModel.setRowCount(0);
        for (ModerationRequest request : myRequests) {
            requestsModel.addRow(new Object[]{
                    request.getCreatedAt().format(DATE_TIME_FORMATTER),
                    "%s (%s)".formatted(request.getActivity().getTitle(), request.getActivity().getStartTime().format(DATE_TIME_FORMATTER)),
                    statusLabel(request.getActivity(), request),
                    Optional.ofNullable(request.getResponseMessage()).orElse(""),
                    Optional.ofNullable(request.getDeclineReason()).orElse("")
            });
        }
    }

    private void updateMyActivities() {
        myActivitiesModel.clear();
        Map<Long, EventActivity> unique = new LinkedHashMap<>();
        for (ModerationRequest request : myRequests) {
            if (request.getStatus() == ModerationRequest.Status.APPROVED) {
                unique.putIfAbsent(request.getActivity().getId(), request.getActivity());
            }
        }
        unique.values().forEach(myActivitiesModel::addElement);
        if (!myActivitiesModel.isEmpty()) {
            myActivitiesList.setSelectedIndex(0);
        } else {
            tasksPanel.setActivity(null);
        }
    }

    private Map<Long, ModerationRequest> collectLatestRequests() {
        Map<Long, ModerationRequest> map = new LinkedHashMap<>();
        for (ModerationRequest request : myRequests) {
            map.putIfAbsent(request.getActivity().getId(), request);
        }
        return map;
    }

    private String statusLabel(EventActivity activity, ModerationRequest request) {
        if (activity.getEndTime().isBefore(LocalDateTime.now())) {
            return "Завершено";
        }
        if (request == null) {
            return "Свободно";
        }
        return switch (request.getStatus()) {
            case PENDING -> "Заявка отправлена";
            case APPROVED -> "Подтверждено";
            case DECLINED -> "Отклонено";
            case CANCELLED -> "Отменено";
        };
    }

    private void submitSelectedRequest() {
        int row = availableTable.getSelectedRow();
        if (row < 0 || row >= availableModel.getRowCount()) {
            JOptionPane.showMessageDialog(this, "Выберите активность", "Внимание", JOptionPane.WARNING_MESSAGE);
            return;
        }
        EventActivity activity = row < filteredActivities.size() ? filteredActivities.get(row) : null;
        if (activity == null) {
            JOptionPane.showMessageDialog(this, "Не удалось определить активность", "Ошибка", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (context.moderatorService().findExistingRequest(activity.getId(), moderator.getId()).isPresent()) {
            JOptionPane.showMessageDialog(this, "Вы уже подали заявку на эту активность", "Информация", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        List<ModerationRequest> conflicts = context.moderatorService().findConflicts(moderator.getId(), activity);
        if (!conflicts.isEmpty()) {
            ConflictDecision decision = showConflictDialog(conflicts);
            if (decision == ConflictDecision.CANCEL_NEW) {
                return;
            }
            if (decision == ConflictDecision.CANCEL_OLD) {
                conflicts = context.moderatorService().findConflicts(moderator.getId(), activity);
                if (!conflicts.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Конфликт времени не устранен. Завершите работу с пересекающимися активностями.", "Внимание", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            }
        }
        context.moderatorService().submitRequest(activity.getId(), moderator.getId(), ModerationRequest.Status.PENDING, null, "Ожидает подтверждения");
        JOptionPane.showMessageDialog(this, "Заявка отправлена и ожидает одобрения организатора", "Готово", JOptionPane.INFORMATION_MESSAGE);
        reloadData();
    }

    private void openTasksForSelected() {
        EventActivity activity = getSelectedActivityFromTable();
        if (activity == null) {
            return;
        }
        loadActivityIntoPanel(activity);
        ActivityTasksDialog dialog = new ActivityTasksDialog(this, context, activity, moderator.getId(), true);
        dialog.setVisible(true);
        dialog.reload();
        reloadData();
    }

    private EventActivity getSelectedActivityFromTable() {
        int row = availableTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Выберите активность", "Внимание", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return row < filteredActivities.size() ? filteredActivities.get(row) : null;
    }

    private void cancelSelectedRequest() {
        int row = requestsTable.getSelectedRow();
        if (row < 0 || row >= myRequests.size()) {
            JOptionPane.showMessageDialog(this, "Выберите заявку", "Внимание", JOptionPane.WARNING_MESSAGE);
            return;
        }
        ModerationRequest request = myRequests.get(row);
        if (request.getStatus() != ModerationRequest.Status.PENDING) {
            JOptionPane.showMessageDialog(this, "Можно отменить только заявку в статусе 'Ожидает'", "Информация", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (JOptionPane.showConfirmDialog(this, "Отменить выбранную заявку?", "Подтверждение", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            context.moderatorService().updateStatus(request.getId(), ModerationRequest.Status.CANCELLED, "Отменено модератором", null);
            reloadData();
        }
    }

    private void loadActivityIntoPanel(EventActivity summary) {
        context.eventService().loadEvent(summary.getEventId()).ifPresent(event -> event.getActivities().stream()
                .filter(a -> a.getId() == summary.getId())
                .findFirst()
                .ifPresent(tasksPanel::setActivity));
    }

    private ConflictDecision showConflictDialog(List<ModerationRequest> conflicts) {
        ConflictDialog dialog = new ConflictDialog(this, conflicts);
        dialog.setVisible(true);
        return dialog.getDecision();
    }

    private enum ConflictDecision {
        CANCEL_OLD,
        CANCEL_NEW,
        NONE
    }

    private class ConflictDialog extends JDialog {
        private final JList<ModerationRequest> conflictsList;
        private ConflictDecision decision = ConflictDecision.NONE;

        ConflictDialog(Window owner, List<ModerationRequest> conflicts) {
            super(owner, "Конфликт расписания", ModalityType.APPLICATION_MODAL);
            setLayout(new BorderLayout(8, 8));
            conflictsList = new JList<>(new DefaultListModel<>());
            DefaultListModel<ModerationRequest> model = (DefaultListModel<ModerationRequest>) conflictsList.getModel();
            conflicts.forEach(model::addElement);
            conflictsList.setCellRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    if (value instanceof ModerationRequest request) {
                        value = "%s (%s) - %s".formatted(request.getActivity().getTitle(), request.getActivity().getStartTime().format(DATE_TIME_FORMATTER), statusLabel(request.getActivity(), request));
                    }
                    return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                }
            });
            add(new JScrollPane(conflictsList), BorderLayout.CENTER);

            JTextArea hint = new JTextArea("Вы уже подали заявку на пересекающуюся активность.\nВыберите заявку для отмены или отмените новую подачу.");
            hint.setEditable(false);
            hint.setLineWrap(true);
            hint.setWrapStyleWord(true);
            hint.setOpaque(false);
            add(hint, BorderLayout.NORTH);

            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton cancelNew = new JButton("Отмена новой заявки");
            cancelNew.addActionListener(e -> {
                decision = ConflictDecision.CANCEL_NEW;
                dispose();
            });
            JButton cancelOld = new JButton("Отменить выбранную");
            cancelOld.addActionListener(e -> {
                ModerationRequest selected = conflictsList.getSelectedValue();
                if (selected == null) {
                    JOptionPane.showMessageDialog(this, "Выберите заявку для отмены", "Внимание", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                context.moderatorService().updateStatus(selected.getId(), ModerationRequest.Status.CANCELLED, "Отменено пользователем при выборе новой активности", null);
                decision = ConflictDecision.CANCEL_OLD;
                dispose();
            });
            JButton close = new JButton("Закрыть");
            close.addActionListener(e -> dispose());
            buttons.add(close);
            buttons.add(cancelNew);
            buttons.add(cancelOld);
            add(buttons, BorderLayout.SOUTH);

            setSize(520, 360);
            setLocationRelativeTo(owner);
        }

        ConflictDecision getDecision() {
            return decision;
        }
    }
}

