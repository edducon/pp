package com.example.eventapp.ui;

import com.example.eventapp.model.Event;
import com.example.eventapp.model.EventActivity;
import com.example.eventapp.service.AppContext;
import com.example.eventapp.util.PdfExporter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class KanbanBoardFrame extends JFrame {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final AppContext context;
    private final long organizerId;
    private final JComboBox<Event> eventBox = new JComboBox<>();
    private final KanbanPanel boardPanel = new KanbanPanel();
    private Event currentEvent;
    private ActivityTasksDialog tasksDialog;

    public KanbanBoardFrame(Frame owner, AppContext context, Event initialEvent) {
        super("Kanban-доска мероприятий");
        this.context = context;
        this.organizerId = initialEvent.getOrganizerId();
        setSize(1000, 720);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        initUi();
        loadEvents(initialEvent);
    }

    private void initUi() {
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.setBorder(new EmptyBorder(8, 8, 8, 8));
        top.add(new JLabel("Мероприятие:"));
        eventBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                String title = value instanceof Event event ? "%s (%s)".formatted(event.getTitle(), event.getStartTime().toLocalDate()) : "-";
                return super.getListCellRendererComponent(list, title, index, isSelected, cellHasFocus);
            }
        });
        eventBox.addActionListener(e -> onEventChanged());
        top.add(eventBox);

        JButton refreshButton = new JButton("Обновить");
        refreshButton.addActionListener(e -> reloadCurrentEvent());
        JButton exportButton = new JButton("Экспорт в PDF");
        exportButton.addActionListener(e -> exportBoard());
        top.add(refreshButton);
        top.add(exportButton);

        add(top, BorderLayout.NORTH);
        JScrollPane scrollPane = new JScrollPane(boardPanel);
        add(scrollPane, BorderLayout.CENTER);
    }

    private void loadEvents(Event preselected) {
        List<Event> events = context.eventService().loadOrganizerEvents(organizerId);
        eventBox.removeAllItems();
        Event selected = null;
        for (Event event : events) {
            eventBox.addItem(event);
            if (preselected != null && event.getId() == preselected.getId()) {
                selected = event;
            }
        }
        if (selected != null) {
            eventBox.setSelectedItem(selected);
        } else if (!events.isEmpty()) {
            eventBox.setSelectedIndex(0);
        }
        reloadCurrentEvent();
    }

    private void onEventChanged() {
        reloadCurrentEvent();
    }

    private void reloadCurrentEvent() {
        Event selected = (Event) eventBox.getSelectedItem();
        if (selected == null) {
            return;
        }
        context.eventService().loadEvent(selected.getId()).ifPresent(event -> {
            currentEvent = event;
            boardPanel.showEvent(event);
        });
    }

    private void exportBoard() {
        if (currentEvent == null) {
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(currentEvent.getTitle().replaceAll("\\s+", "_") + "_kanban.pdf"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                PdfExporter.exportPanel(boardPanel, chooser.getSelectedFile());
                JOptionPane.showMessageDialog(this, "Файл сформирован", "Готово", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Не удалось создать PDF: " + ex.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private class KanbanPanel extends JPanel {
        KanbanPanel() {
            setLayout(null);
            setPreferredSize(new Dimension(900, 600));
            setBackground(new Color(245, 245, 245));
        }

        void showEvent(Event event) {
            removeAll();
            int y = 20;
            int columnWidth = 280;
            int column = 0;
            int rowHeight = 160;
            for (EventActivity activity : event.getActivities()) {
                ActivityCard card = new ActivityCard(activity);
                int x = 20 + column * (columnWidth + 20);
                card.setBounds(x, y, columnWidth, 140);
                add(card);
                column++;
                if (column == 3) {
                    column = 0;
                    y += rowHeight;
                }
            }
            int rows = (int) Math.ceil(event.getActivities().size() / 3.0);
            int height = Math.max(600, rows * rowHeight + 40);
            setPreferredSize(new Dimension(900, height));
            revalidate();
            repaint();
        }
    }

    private class ActivityCard extends JPanel {
        private final EventActivity activity;
        private Point dragOffset;

        ActivityCard(EventActivity activity) {
            this.activity = activity;
            setLayout(new BorderLayout(4, 4));
            setBorder(BorderFactory.createLineBorder(new Color(0x4A90E2), 2, true));
            setBackground(Color.WHITE);
            JLabel title = new JLabel("<html><b>%s</b></html>".formatted(activity.getTitle()));
            add(title, BorderLayout.NORTH);

            JTextArea info = new JTextArea();
            info.setEditable(false);
            info.setWrapStyleWord(true);
            info.setLineWrap(true);
            String jury = activity.getJuryMembers().isEmpty() ? "-" : activity.getJuryMembers().stream()
                    .map(j -> j.getFullName().toString())
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("-");
            info.setText("%s - %s\nНаправление: %s\nЖюри: %s".formatted(
                    activity.getStartTime().format(TIME_FORMATTER),
                    activity.getEndTime().format(TIME_FORMATTER),
                    activity.getEventDirection() == null ? "-" : activity.getEventDirection().getName(),
                    jury));
            add(info, BorderLayout.CENTER);

            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    dragOffset = e.getPoint();
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                        openTasks();
                    }
                }
            });

            addMouseMotionListener(new MouseAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    if (dragOffset == null) {
                        return;
                    }
                    Point newLocation = getLocation();
                    newLocation.translate(e.getX() - dragOffset.x, e.getY() - dragOffset.y);
                    newLocation.x = Math.max(0, Math.min(newLocation.x, boardPanel.getWidth() - getWidth()));
                    newLocation.y = Math.max(0, Math.min(newLocation.y, boardPanel.getHeight() - getHeight()));
                    setLocation(newLocation);
                }
            });
        }

        private void openTasks() {
            if (currentEvent == null) {
                return;
            }
            context.eventService().loadEvent(currentEvent.getId()).ifPresent(event -> event.getActivities().stream()
                    .filter(a -> a.getId() == activity.getId())
                    .findFirst()
                    .ifPresent(actual -> {
                        if (tasksDialog == null) {
                            tasksDialog = new ActivityTasksDialog(KanbanBoardFrame.this, context, actual, null, true);
                        } else {
                            tasksDialog.setActivity(actual);
                            tasksDialog.reload();
                        }
                        tasksDialog.setVisible(true);
                    }));
        }
    }
}

