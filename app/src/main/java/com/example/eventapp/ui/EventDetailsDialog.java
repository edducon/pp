package com.example.eventapp.ui;

import com.example.eventapp.model.Event;
import com.example.eventapp.model.EventActivity;
import com.example.eventapp.service.AppContext;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;

public class EventDetailsDialog extends JDialog {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    public EventDetailsDialog(Frame owner, AppContext context, Event event) {
        super(owner, event.getTitle(), true);
        setSize(760, 520);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());

        JPanel header = new JPanel(new BorderLayout(10, 10));
        header.setBorder(new EmptyBorder(10, 10, 10, 10));
        JLabel logoLabel = new JLabel(loadLogo(event.getLogoPath()));
        header.add(logoLabel, BorderLayout.WEST);

        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.add(new JLabel("Направление: " + event.getDirection().getName()));
        infoPanel.add(new JLabel("Период: %s - %s".formatted(
                event.getStartTime().format(DATE_TIME_FORMATTER),
                event.getEndTime().format(DATE_TIME_FORMATTER))));
        String cityName = context.cityDao().findById(event.getCityId()).map(city -> city.getName()).orElse("-");
        infoPanel.add(new JLabel("Город: " + cityName));
        JTextArea description = new JTextArea(event.getDescription() == null ? "" : event.getDescription());
        description.setEditable(false);
        description.setLineWrap(true);
        description.setWrapStyleWord(true);
        description.setBorder(BorderFactory.createTitledBorder("Описание"));
        infoPanel.add(description);
        header.add(infoPanel, BorderLayout.CENTER);

        add(header, BorderLayout.NORTH);

        DefaultTableModel model = new DefaultTableModel(new Object[]{"Активность", "Начало", "Окончание", "Жюри"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable table = new JTable(model);
        table.setRowHeight(26);
        for (EventActivity activity : event.getActivities()) {
            String jury = activity.getJuryMembers().stream()
                    .map(j -> j.getFullName().toString())
                    .reduce((a, b) -> a + "; " + b)
                    .orElse("-");
            model.addRow(new Object[]{activity.getTitle(),
                    activity.getStartTime().format(DATE_TIME_FORMATTER),
                    activity.getEndTime().format(DATE_TIME_FORMATTER), jury});
        }
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    private Icon loadLogo(String path) {
        if (path == null || path.isBlank()) {
            return UIManager.getIcon("FileView.fileIcon");
        }
        try {
            Path filePath = Path.of(path);
            if (Files.exists(filePath)) {
                Image image = new ImageIcon(path).getImage().getScaledInstance(120, 120, Image.SCALE_SMOOTH);
                return new ImageIcon(image);
            }
        } catch (Exception ignored) {
        }
        return UIManager.getIcon("FileView.fileIcon");
    }
}
