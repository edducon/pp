package com.example.eventapp.ui;

import com.example.eventapp.model.ActivityResource;
import com.example.eventapp.model.ActivityTask;
import com.example.eventapp.model.Event;
import com.example.eventapp.model.EventActivity;
import com.example.eventapp.model.Participant;
import com.example.eventapp.service.AppContext;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Panel that exposes task/resource management for a single activity. Used from the moderator dashboard
 * and Kanban dialog, so all mutations are optional and controlled via the {@code allowEdits} flag.
 */
public class ActivityTasksPanel extends JPanel {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final AppContext context;
    private final Long actingModeratorId;
    private final boolean allowEdits;

    private EventActivity activity;
    private final DefaultTableModel tasksModel = new DefaultTableModel(new Object[]{"Название", "Участник", "Создано"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable tasksTable = new JTable(tasksModel);
    private final DefaultTableModel resourcesModel = new DefaultTableModel(new Object[]{"Название", "Путь", "Загружено", "Автор"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable resourcesTable = new JTable(resourcesModel);

    private List<ActivityTask> tasks = new ArrayList<>();
    private List<ActivityResource> resources = new ArrayList<>();

    public ActivityTasksPanel(AppContext context, Long actingModeratorId, boolean allowEdits) {
        super(new BorderLayout(8, 8));
        this.context = context;
        this.actingModeratorId = actingModeratorId;
        this.allowEdits = allowEdits;
        initUi();
    }

    private void initUi() {
        setBorder(new EmptyBorder(10, 10, 10, 10));

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Задачи", createTasksPanel());
        tabs.addTab("Ресурсы", createResourcesPanel());
        add(tabs, BorderLayout.CENTER);
    }

    private JPanel createTasksPanel() {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        tasksTable.setRowHeight(26);
        tasksTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        panel.add(new JScrollPane(tasksTable), BorderLayout.CENTER);

        JTextArea descriptionArea = new JTextArea(4, 20);
        descriptionArea.setEditable(false);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        tasksTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = tasksTable.getSelectedRow();
                if (row >= 0 && row < tasks.size()) {
                    descriptionArea.setText(tasks.get(row).getDescription());
                } else {
                    descriptionArea.setText("");
                }
            }
        });
        panel.add(new JScrollPane(descriptionArea), BorderLayout.SOUTH);

        if (allowEdits) {
            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton add = new JButton("Добавить задачу");
            add.addActionListener(e -> openTaskDialog());
            JButton remove = new JButton("Удалить");
            remove.addActionListener(e -> deleteSelectedTask());
            buttons.add(remove);
            buttons.add(add);
            panel.add(buttons, BorderLayout.NORTH);
        }
        return panel;
    }

    private JPanel createResourcesPanel() {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        resourcesTable.setRowHeight(26);
        resourcesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        panel.add(new JScrollPane(resourcesTable), BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton download = new JButton("Скачать");
        download.addActionListener(e -> downloadSelectedResource());
        buttons.add(download);
        if (allowEdits) {
            JButton add = new JButton("Добавить ресурс");
            add.addActionListener(e -> openResourceDialog());
            JButton remove = new JButton("Удалить");
            remove.addActionListener(e -> deleteSelectedResource());
            buttons.add(remove);
            buttons.add(add);
        }
        panel.add(buttons, BorderLayout.NORTH);
        return panel;
    }

    public void setActivity(EventActivity activity) {
        this.activity = activity;
        if (activity == null) {
            tasks = new ArrayList<>();
            resources = new ArrayList<>();
            refreshTables();
        } else {
            reloadFromDatabase();
        }
    }

    public void reloadFromDatabase() {
        if (activity == null) {
            return;
        }
        Optional<Event> eventOpt = context.eventService().loadEvent(activity.getEventId());
        eventOpt.ifPresent(event -> event.getActivities().stream()
                .filter(a -> a.getId() == activity.getId())
                .findFirst()
                .ifPresent(this::updateActivitySnapshot));
    }

    private void updateActivitySnapshot(EventActivity latest) {
        this.activity = latest;
        this.tasks = new ArrayList<>(latest.getTasks());
        this.resources = new ArrayList<>(latest.getResources());
        refreshTables();
    }

    private void refreshTables() {
        tasksModel.setRowCount(0);
        for (ActivityTask task : tasks) {
            String participant = task.getParticipant() == null ? "-" : task.getParticipant().getFullName().toString();
            tasksModel.addRow(new Object[]{task.getTitle(), participant, task.getCreatedAt().format(DATE_TIME_FORMATTER)});
        }
        resourcesModel.setRowCount(0);
        for (ActivityResource resource : resources) {
            String author = resource.getUploadedBy() == null ? "-" : resource.getUploadedBy().getFullName().toString();
            resourcesModel.addRow(new Object[]{resource.getName(), resource.getResourcePath(), resource.getUploadedAt().format(DATE_TIME_FORMATTER), author});
        }
    }

    private void openTaskDialog() {
        if (activity == null) {
            return;
        }
        TaskEditorDialog dialog = new TaskEditorDialog(SwingUtilities.getWindowAncestor(this), context.participantDao().findAll());
        dialog.setVisible(true);
        dialog.getResult().ifPresent(result -> {
            ActivityTask created = context.activityManagementDao().addTask(activity.getId(), result.title(), result.description(), result.participantId());
            tasks.add(created);
            refreshTables();
        });
    }

    private void deleteSelectedTask() {
        int row = tasksTable.getSelectedRow();
        if (row < 0 || row >= tasks.size()) {
            JOptionPane.showMessageDialog(this, "Выберите задачу", "Внимание", JOptionPane.WARNING_MESSAGE);
            return;
        }
        ActivityTask task = tasks.get(row);
        if (JOptionPane.showConfirmDialog(this, "Удалить задачу '%s'?".formatted(task.getTitle()), "Подтверждение", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            context.activityManagementDao().deleteTask(task.getId());
            tasks.remove(row);
            refreshTables();
        }
    }

    private void openResourceDialog() {
        if (activity == null) {
            return;
        }
        ResourceEditorDialog dialog = new ResourceEditorDialog(SwingUtilities.getWindowAncestor(this));
        dialog.setVisible(true);
        dialog.getResult().ifPresent(result -> {
            ActivityResource resource = context.activityManagementDao().addResource(activity.getId(), result.name(), result.path(), actingModeratorId);
            resources.add(resource);
            refreshTables();
        });
    }

    private void deleteSelectedResource() {
        int row = resourcesTable.getSelectedRow();
        if (row < 0 || row >= resources.size()) {
            JOptionPane.showMessageDialog(this, "Выберите ресурс", "Внимание", JOptionPane.WARNING_MESSAGE);
            return;
        }
        ActivityResource resource = resources.get(row);
        if (JOptionPane.showConfirmDialog(this, "Удалить ресурс '%s'?".formatted(resource.getName()), "Подтверждение", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            context.activityManagementDao().deleteResource(resource.getId());
            resources.remove(row);
            refreshTables();
        }
    }

    private void downloadSelectedResource() {
        int row = resourcesTable.getSelectedRow();
        if (row < 0 || row >= resources.size()) {
            JOptionPane.showMessageDialog(this, "Выберите ресурс", "Внимание", JOptionPane.WARNING_MESSAGE);
            return;
        }
        ActivityResource resource = resources.get(row);
        if (resource.getResourcePath() == null || resource.getResourcePath().isBlank()) {
            JOptionPane.showMessageDialog(this, "Для ресурса не указан путь", "Ошибка", JOptionPane.ERROR_MESSAGE);
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(resource.getName()));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File source = new File(resource.getResourcePath());
            if (!source.exists()) {
                JOptionPane.showMessageDialog(this, "Файл не найден. Убедитесь, что он расположен по пути: " + resource.getResourcePath(), "Ошибка", JOptionPane.ERROR_MESSAGE);
                return;
            }
            try {
                Files.copy(source.toPath(), chooser.getSelectedFile().toPath(), StandardCopyOption.REPLACE_EXISTING);
                JOptionPane.showMessageDialog(this, "Ресурс сохранен", "Готово", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Не удалось сохранить ресурс: " + ex.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private record TaskFormResult(String title, String description, Long participantId) {}

    private record ResourceFormResult(String name, String path) {}

    private static class TaskEditorDialog extends JDialog {
        private final JTextField titleField = new JTextField(20);
        private final JTextArea descriptionArea = new JTextArea(4, 20);
        private final JComboBox<Participant> participantBox = new JComboBox<>();
        private Optional<TaskFormResult> result = Optional.empty();

        TaskEditorDialog(Window owner, List<Participant> participants) {
            super(owner, "Новая задача", ModalityType.APPLICATION_MODAL);
            setLayout(new BorderLayout(8, 8));
            JPanel form = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(4, 4, 4, 4);
            gbc.anchor = GridBagConstraints.WEST;
            gbc.gridx = 0;
            gbc.gridy = 0;
            form.add(new JLabel("Название:"), gbc);
            gbc.gridx = 1;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1;
            form.add(titleField, gbc);

            gbc.gridx = 0;
            gbc.gridy++;
            gbc.weightx = 0;
            gbc.fill = GridBagConstraints.NONE;
            form.add(new JLabel("Описание:"), gbc);
            gbc.gridx = 1;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            descriptionArea.setLineWrap(true);
            descriptionArea.setWrapStyleWord(true);
            form.add(new JScrollPane(descriptionArea), gbc);

            gbc.gridx = 0;
            gbc.gridy++;
            gbc.fill = GridBagConstraints.NONE;
            form.add(new JLabel("Участник:"), gbc);
            gbc.gridx = 1;
            participantBox.addItem(null);
            participants.forEach(participantBox::addItem);
            participantBox.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    String label = value instanceof Participant participant ? participant.getFullName().toString() : "Не указан";
                    return super.getListCellRendererComponent(list, label, index, isSelected, cellHasFocus);
                }
            });
            form.add(participantBox, gbc);

            add(form, BorderLayout.CENTER);

            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton cancel = new JButton("Отмена");
            cancel.addActionListener(e -> dispose());
            JButton ok = new JButton("Сохранить");
            ok.addActionListener(e -> save());
            buttons.add(cancel);
            buttons.add(ok);
            add(buttons, BorderLayout.SOUTH);

            pack();
            setLocationRelativeTo(owner);
        }

        private void save() {
            if (titleField.getText().isBlank()) {
                JOptionPane.showMessageDialog(this, "Введите название задачи", "Ошибка", JOptionPane.ERROR_MESSAGE);
                return;
            }
            Participant participant = (Participant) participantBox.getSelectedItem();
            Long participantId = participant == null ? null : participant.getId();
            result = Optional.of(new TaskFormResult(titleField.getText().trim(), descriptionArea.getText().trim(), participantId));
            dispose();
        }

        public Optional<TaskFormResult> getResult() {
            return result;
        }
    }

    private static class ResourceEditorDialog extends JDialog {
        private final JTextField nameField = new JTextField(20);
        private final JTextField pathField = new JTextField(25);
        private Optional<ResourceFormResult> result = Optional.empty();

        ResourceEditorDialog(Window owner) {
            super(owner, "Новый ресурс", ModalityType.APPLICATION_MODAL);
            setLayout(new BorderLayout(8, 8));
            JPanel form = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(4, 4, 4, 4);
            gbc.anchor = GridBagConstraints.WEST;
            gbc.gridx = 0;
            gbc.gridy = 0;
            form.add(new JLabel("Название:"), gbc);
            gbc.gridx = 1;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1;
            form.add(nameField, gbc);

            gbc.gridx = 0;
            gbc.gridy++;
            gbc.weightx = 0;
            gbc.fill = GridBagConstraints.NONE;
            form.add(new JLabel("Путь к файлу:"), gbc);
            gbc.gridx = 1;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            form.add(pathField, gbc);
            JButton browse = new JButton("Обзор");
            browse.addActionListener(e -> browseFile());
            gbc.gridx = 2;
            gbc.fill = GridBagConstraints.NONE;
            form.add(browse, gbc);

            add(form, BorderLayout.CENTER);

            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton cancel = new JButton("Отмена");
            cancel.addActionListener(e -> dispose());
            JButton ok = new JButton("Сохранить");
            ok.addActionListener(e -> save());
            buttons.add(cancel);
            buttons.add(ok);
            add(buttons, BorderLayout.SOUTH);

            pack();
            setLocationRelativeTo(owner);
        }

        private void browseFile() {
            JFileChooser chooser = new JFileChooser();
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                pathField.setText(chooser.getSelectedFile().getAbsolutePath());
                if (nameField.getText().isBlank()) {
                    nameField.setText(chooser.getSelectedFile().getName());
                }
            }
        }

        private void save() {
            if (nameField.getText().isBlank()) {
                JOptionPane.showMessageDialog(this, "Введите название ресурса", "Ошибка", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (pathField.getText().isBlank()) {
                JOptionPane.showMessageDialog(this, "Укажите путь к файлу", "Ошибка", JOptionPane.ERROR_MESSAGE);
                return;
            }
            result = Optional.of(new ResourceFormResult(nameField.getText().trim(), pathField.getText().trim()));
            dispose();
        }

        public Optional<ResourceFormResult> getResult() {
            return result;
        }
    }
}

