package com.example.eventapp.ui;

import com.example.eventapp.model.AuthenticatedUser;
import com.example.eventapp.model.Event;
import com.example.eventapp.model.Participant;
import com.example.eventapp.model.Role;
import com.example.eventapp.service.AppContext;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ParticipantDirectoryFrame extends JFrame {
    private final AppContext context;
    private final Optional<AuthenticatedUser> user;
    private final JTextField lastNameField = new JTextField(15);
    private final JComboBox<Event> eventBox = new JComboBox<>();
    private final JLabel countLabel = new JLabel();
    private final DefaultTableModel model = new DefaultTableModel(new Object[]{"ID", "ФИО", "Email", "Телефон", "Фото"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    public ParticipantDirectoryFrame(Frame owner, AppContext context, Optional<AuthenticatedUser> user) {
        super("Участники мероприятий");
        this.context = context;
        this.user = user;
        setSize(780, 520);
        setLocationRelativeTo(owner);
        initUi();
        loadEvents();
        loadParticipants();
    }

    private void initUi() {
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterPanel.add(new JLabel("Фамилия:"));
        filterPanel.add(lastNameField);
        filterPanel.add(new JLabel("Мероприятие:"));
        eventBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                String title = value instanceof Event event ? event.getTitle() : "Все";
                return super.getListCellRendererComponent(list, title, index, isSelected, cellHasFocus);
            }
        });
        eventBox.addItem(null);
        filterPanel.add(eventBox);
        JButton filterButton = new JButton("Поиск");
        filterButton.addActionListener(e -> loadParticipants());
        filterPanel.add(filterButton);
        JButton resetButton = new JButton("Сброс");
        resetButton.addActionListener(e -> {
            lastNameField.setText("");
            eventBox.setSelectedIndex(0);
            loadParticipants();
        });
        filterPanel.add(resetButton);

        JTable table = new JTable(model);
        table.setRowHeight(26);
        JScrollPane scrollPane = new JScrollPane(table);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottomPanel.add(countLabel);
        JButton registerButton = new JButton("Регистрация участника");
        registerButton.addActionListener(e -> ParticipantRegistrationDialog.open(this, context));
        if (user.map(u -> u.getRole() == Role.ORGANIZER).orElse(false) || user.isEmpty()) {
            bottomPanel.add(registerButton);
        }

        add(filterPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void loadEvents() {
        eventBox.removeAllItems();
        eventBox.addItem(null);
        List<Event> events = new ArrayList<>();
        if (user.isPresent() && user.get().getRole() == Role.ORGANIZER) {
            events.addAll(context.eventService().loadOrganizerEvents(user.get().getAccount().getId()));
        } else {
            events.addAll(context.eventService().loadMainEvents(null, null));
        }
        events.forEach(eventBox::addItem);
    }

    private void loadParticipants() {
        String lastName = lastNameField.getText().trim();
        Event selected = (Event) eventBox.getSelectedItem();
        Long eventId = selected == null ? null : selected.getId();
        List<Participant> participants = context.participantDao().search(lastName.isBlank() ? null : lastName, eventId);
        model.setRowCount(0);
        for (Participant participant : participants) {
            model.addRow(new Object[]{participant.getIdNumber(), participant.getFullName().toString(), participant.getEmail(), participant.getPhone(), participant.getPhotoPath()});
        }
        countLabel.setText("Всего: " + participants.size());
    }
}
