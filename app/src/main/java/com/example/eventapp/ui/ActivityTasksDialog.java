package com.example.eventapp.ui;

import com.example.eventapp.model.EventActivity;
import com.example.eventapp.service.AppContext;

import javax.swing.*;
import java.awt.*;

public class ActivityTasksDialog extends JDialog {
    private final ActivityTasksPanel panel;

    public ActivityTasksDialog(Window owner, AppContext context, EventActivity activity, Long moderatorId, boolean allowEdits) {
        super(owner, "Задачи и ресурсы", ModalityType.MODELESS);
        this.panel = new ActivityTasksPanel(context, moderatorId, allowEdits);
        setLayout(new BorderLayout());
        add(panel, BorderLayout.CENTER);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setSize(720, 520);
        setLocationRelativeTo(owner);
        panel.setActivity(activity);
    }

    public void setActivity(EventActivity activity) {
        panel.setActivity(activity);
    }

    public void reload() {
        panel.reloadFromDatabase();
    }
}

