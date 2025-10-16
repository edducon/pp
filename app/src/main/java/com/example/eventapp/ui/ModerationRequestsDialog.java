package com.example.eventapp.ui;

import com.example.eventapp.model.Event;
import com.example.eventapp.model.ModerationRequest;
import com.example.eventapp.service.AppContext;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ModerationRequestsDialog extends JDialog {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final AppContext context;
    private final Event event;
    private final DefaultTableModel model = new DefaultTableModel(new Object[]{"Создана", "Активность", "Модератор", "Статус", "Комментарий", "Причина"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);
    private final JLabel counterLabel = new JLabel();
    private List<ModerationRequest> requests = new ArrayList<>();

    public ModerationRequestsDialog(Frame owner, AppContext context, Event event) {
        super(owner, "Заявки модераторов", true);
        this.context = context;
        this.event = event;
        setSize(860, 520);
        setLocationRelativeTo(owner);
        initUi();
        loadData();
    }

    private void initUi() {
        setLayout(new BorderLayout(8, 8));
        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
        header.add(new JLabel("Мероприятие: " + event.getTitle()), BorderLayout.WEST);
        add(header, BorderLayout.NORTH);

        table.setRowHeight(26);
        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton approve = new JButton("Подтвердить");
        approve.addActionListener(e -> approveSelected());
        JButton decline = new JButton("Отклонить");
        decline.addActionListener(e -> declineSelected());
        JButton refresh = new JButton("Обновить");
        refresh.addActionListener(e -> loadData());
        buttons.add(refresh);
        buttons.add(decline);
        buttons.add(approve);
        bottom.add(buttons, BorderLayout.EAST);
        counterLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        bottom.add(counterLabel, BorderLayout.WEST);
        add(bottom, BorderLayout.SOUTH);
    }

    private void loadData() {
        requests = context.moderationRequestDao().findByEvent(event.getId());
        model.setRowCount(0);
        for (ModerationRequest request : requests) {
            String moderatorName = request.getModerator().getFullName().toString();
            String status = switch (request.getStatus()) {
                case PENDING -> "Ожидает";
                case APPROVED -> "Одобрено";
                case DECLINED -> "Отклонено";
                case CANCELLED -> "Отменено";
            };
            model.addRow(new Object[]{
                    request.getCreatedAt().format(DATE_TIME_FORMATTER),
                    "%s (%s)".formatted(request.getActivity().getTitle(), request.getActivity().getStartTime().format(DATE_TIME_FORMATTER)),
                    moderatorName,
                    status,
                    request.getResponseMessage() == null ? "" : request.getResponseMessage(),
                    request.getDeclineReason() == null ? "" : request.getDeclineReason()
            });
        }
        counterLabel.setText("Всего заявок: " + requests.size());
    }

    private void approveSelected() {
        ModerationRequest request = getSelectedRequest();
        if (request == null) {
            return;
        }
        if (request.getStatus() == ModerationRequest.Status.APPROVED) {
            JOptionPane.showMessageDialog(this, "Заявка уже подтверждена", "Информация", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (request.getStatus() == ModerationRequest.Status.CANCELLED || request.getStatus() == ModerationRequest.Status.DECLINED) {
            JOptionPane.showMessageDialog(this, "Заявка уже закрыта", "Информация", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String message = JOptionPane.showInputDialog(this, "Сообщение модератору (необязательно)");
        context.moderatorService().updateStatus(request.getId(), ModerationRequest.Status.APPROVED, message, null);
        loadData();
    }

    private void declineSelected() {
        ModerationRequest request = getSelectedRequest();
        if (request == null) {
            return;
        }
        if (request.getStatus() == ModerationRequest.Status.DECLINED) {
            JOptionPane.showMessageDialog(this, "Заявка уже отклонена", "Информация", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (request.getStatus() == ModerationRequest.Status.CANCELLED) {
            JOptionPane.showMessageDialog(this, "Заявка отменена модератором", "Информация", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String reason = JOptionPane.showInputDialog(this, "Укажите причину отказа");
        if (reason == null || reason.isBlank()) {
            JOptionPane.showMessageDialog(this, "Необходимо указать причину", "Ошибка", JOptionPane.ERROR_MESSAGE);
            return;
        }
        context.moderatorService().updateStatus(request.getId(), ModerationRequest.Status.DECLINED, null, reason.trim());
        loadData();
    }

    private ModerationRequest getSelectedRequest() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= requests.size()) {
            JOptionPane.showMessageDialog(this, "Выберите заявку", "Внимание", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return requests.get(row);
    }
}

