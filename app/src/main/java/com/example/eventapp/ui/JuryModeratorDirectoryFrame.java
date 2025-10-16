package com.example.eventapp.ui;

import com.example.eventapp.model.Event;
import com.example.eventapp.model.JuryMember;
import com.example.eventapp.model.Moderator;
import com.example.eventapp.model.Role;
import com.example.eventapp.service.AppContext;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class JuryModeratorDirectoryFrame extends JFrame {
    private final AppContext context;

    public JuryModeratorDirectoryFrame(Frame owner, AppContext context) {
        super("Жюри и модераторы");
        this.context = context;
        setSize(900, 540);
        setLocationRelativeTo(owner);
        initUi();
    }

    private void initUi() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Жюри", createJuryPanel());
        tabs.addTab("Модераторы", createModeratorPanel());
        add(tabs, BorderLayout.CENTER);
    }

    private JPanel createJuryPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JTextField lastNameField = new JTextField(12);
        JComboBox<Event> eventBox = createEventBox();
        JLabel countLabel = new JLabel();
        DefaultTableModel model = new DefaultTableModel(new Object[]{"ID", "ФИО", "Email", "Телефон", "Направление"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable table = new JTable(model);
        table.setRowHeight(26);

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filters.add(new JLabel("Фамилия:"));
        filters.add(lastNameField);
        filters.add(new JLabel("Мероприятие:"));
        filters.add(eventBox);
        JButton searchButton = new JButton("Поиск");
        searchButton.addActionListener(e -> loadJury(model, countLabel, lastNameField.getText(), (Event) eventBox.getSelectedItem()));
        JButton resetButton = new JButton("Сброс");
        resetButton.addActionListener(e -> {
            lastNameField.setText("");
            eventBox.setSelectedIndex(0);
            loadJury(model, countLabel, "", null);
        });
        filters.add(searchButton);
        filters.add(resetButton);

        JButton registerButton = new JButton("Регистрация жюри");
        registerButton.addActionListener(e -> JuryRegistrationDialog.open(this, context));
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottom.add(countLabel);
        bottom.add(registerButton);

        panel.add(filters, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(bottom, BorderLayout.SOUTH);
        loadJury(model, countLabel, "", null);
        return panel;
    }

    private JPanel createModeratorPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JTextField lastNameField = new JTextField(12);
        JComboBox<Event> eventBox = createEventBox();
        JLabel countLabel = new JLabel();
        DefaultTableModel model = new DefaultTableModel(new Object[]{"ID", "ФИО", "Email", "Телефон", "Направление"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable table = new JTable(model);
        table.setRowHeight(26);

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filters.add(new JLabel("Фамилия:"));
        filters.add(lastNameField);
        filters.add(new JLabel("Мероприятие:"));
        filters.add(eventBox);
        JButton searchButton = new JButton("Поиск");
        searchButton.addActionListener(e -> loadModerators(model, countLabel, lastNameField.getText(), (Event) eventBox.getSelectedItem()));
        JButton resetButton = new JButton("Сброс");
        resetButton.addActionListener(e -> {
            lastNameField.setText("");
            eventBox.setSelectedIndex(0);
            loadModerators(model, countLabel, "", null);
        });
        filters.add(searchButton);
        filters.add(resetButton);

        JButton registerButton = new JButton("Регистрация модератора");
        registerButton.addActionListener(e -> ModeratorRegistrationDialog.open(this, context));
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottom.add(countLabel);
        bottom.add(registerButton);

        panel.add(filters, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(bottom, BorderLayout.SOUTH);
        loadModerators(model, countLabel, "", null);
        return panel;
    }

    private JComboBox<Event> createEventBox() {
        JComboBox<Event> box = new JComboBox<>();
        box.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                String title = value instanceof Event event ? event.getTitle() : "Все";
                return super.getListCellRendererComponent(list, title, index, isSelected, cellHasFocus);
            }
        });
        box.addItem(null);
        List<Event> events = new ArrayList<>(context.eventService().loadMainEvents(null, null));
        events.forEach(box::addItem);
        return box;
    }

    private void loadJury(DefaultTableModel model, JLabel countLabel, String lastName, Event event) {
        model.setRowCount(0);
        Long eventId = event == null ? null : event.getId();
        List<JuryMember> members = context.juryMemberDao().search(lastName == null || lastName.isBlank() ? null : lastName, eventId);
        for (JuryMember member : members) {
            model.addRow(new Object[]{member.getIdNumber(), member.getFullName().toString(), member.getEmail(), member.getPhone(), member.getDirection() == null ? "-" : member.getDirection().getName()});
        }
        countLabel.setText("Всего: " + members.size());
    }

    private void loadModerators(DefaultTableModel model, JLabel countLabel, String lastName, Event event) {
        model.setRowCount(0);
        Long eventId = event == null ? null : event.getId();
        List<Moderator> moderators = context.moderatorDao().search(lastName == null || lastName.isBlank() ? null : lastName, eventId);
        for (Moderator moderator : moderators) {
            model.addRow(new Object[]{moderator.getIdNumber(), moderator.getFullName().toString(), moderator.getEmail(), moderator.getPhone(), moderator.getDirection() == null ? "-" : moderator.getDirection().getName()});
        }
        countLabel.setText("Всего: " + moderators.size());
    }
}
