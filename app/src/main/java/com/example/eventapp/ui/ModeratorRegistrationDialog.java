package com.example.eventapp.ui;

import com.example.eventapp.model.FullName;
import com.example.eventapp.model.Gender;
import com.example.eventapp.model.Moderator;
import com.example.eventapp.service.AppContext;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

public class ModeratorRegistrationDialog extends JDialog {
    private final AppContext context;
    private final JTextField idField = new JTextField(12);
    private final JTextField lastNameField = new JTextField(20);
    private final JTextField firstNameField = new JTextField(20);
    private final JTextField middleNameField = new JTextField(20);
    private final JComboBox<String> genderBox = new JComboBox<>(new String[]{Gender.MALE.getDisplayName(), Gender.FEMALE.getDisplayName()});
    private final JSpinner birthDateSpinner = new JSpinner(new SpinnerDateModel());
    private final JTextField emailField = new JTextField(25);
    private final JTextField phoneField = new JTextField(20);
    private final JTextField countryField = new JTextField("RU", 4);
    private final JComboBox<String> cityField = new JComboBox<>();
    private final JComboBox<String> directionField = new JComboBox<>();
    private final JTextField photoField = new JTextField(25);
    private final JPasswordField passwordField = new JPasswordField(16);
    private final JPasswordField confirmPasswordField = new JPasswordField(16);

    public static void open(Frame owner, AppContext context) {
        ModeratorRegistrationDialog dialog = new ModeratorRegistrationDialog(owner, context);
        dialog.setVisible(true);
    }

    private ModeratorRegistrationDialog(Frame owner, AppContext context) {
        super(owner, "Регистрация модератора", true);
        this.context = context;
        setSize(560, 440);
        setLocationRelativeTo(owner);
        initUi();
    }

    private void initUi() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;
        int row = 0;
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("ID Number:"), gbc);
        gbc.gridx = 1;
        idField.setEditable(false);
        idField.setText(context.registrationService().generateId("MOD"));
        panel.add(idField, gbc);

        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Фамилия:"), gbc);
        gbc.gridx = 1;
        panel.add(lastNameField, gbc);

        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Имя:"), gbc);
        gbc.gridx = 1;
        panel.add(firstNameField, gbc);

        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Отчество:"), gbc);
        gbc.gridx = 1;
        panel.add(middleNameField, gbc);

        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Пол:"), gbc);
        gbc.gridx = 1;
        panel.add(genderBox, gbc);

        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Дата рождения:"), gbc);
        gbc.gridx = 1;
        birthDateSpinner.setEditor(new JSpinner.DateEditor(birthDateSpinner, "dd.MM.yyyy"));
        panel.add(birthDateSpinner, gbc);

        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Email:"), gbc);
        gbc.gridx = 1;
        panel.add(emailField, gbc);

        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Телефон:"), gbc);
        gbc.gridx = 1;
        panel.add(phoneField, gbc);

        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Страна:"), gbc);
        gbc.gridx = 1;
        panel.add(countryField, gbc);

        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Город:"), gbc);
        gbc.gridx = 1;
        cityField.setEditable(true);
        context.cityDao().findAll().forEach(city -> cityField.addItem(city.getName()));
        panel.add(cityField, gbc);

        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Направление:"), gbc);
        gbc.gridx = 1;
        directionField.setEditable(true);
        context.directionDao().findAll().forEach(direction -> directionField.addItem(direction.getName()));
        panel.add(directionField, gbc);

        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Фото:"), gbc);
        gbc.gridx = 1;
        panel.add(photoField, gbc);

        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Пароль:"), gbc);
        gbc.gridx = 1;
        panel.add(passwordField, gbc);

        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Повторите пароль:"), gbc);
        gbc.gridx = 1;
        panel.add(confirmPasswordField, gbc);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancelButton = new JButton("Отмена");
        cancelButton.addActionListener(e -> dispose());
        JButton saveButton = new JButton("Сохранить");
        saveButton.addActionListener(e -> saveModerator());
        buttons.add(cancelButton);
        buttons.add(saveButton);

        add(panel, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
    }

    private void saveModerator() {
        if (!String.valueOf(passwordField.getPassword()).equals(String.valueOf(confirmPasswordField.getPassword()))) {
            JOptionPane.showMessageDialog(this, "Пароли не совпадают", "Ошибка", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String password = new String(passwordField.getPassword());
        if (!context.registrationService().isPasswordValid(password)) {
            JOptionPane.showMessageDialog(this, "Пароль не соответствует требованиям", "Ошибка", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String cityName = (String) cityField.getSelectedItem();
        if (cityName == null || cityName.isBlank()) {
            JOptionPane.showMessageDialog(this, "Укажите город", "Ошибка", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int cityId = context.cityDao().findByName(cityName).orElseGet(() -> context.cityDao().insertCity(cityName, countryField.getText().trim())).getId();
        LocalDate birthDate = ((Date) birthDateSpinner.getValue()).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        Moderator moderator = context.registrationService().registerModerator(idField.getText(),
                new FullName(lastNameField.getText(), firstNameField.getText(), middleNameField.getText()),
                emailField.getText(),
                birthDate,
                countryField.getText().trim(),
                cityId,
                phoneField.getText(),
                genderBox.getSelectedIndex() == 0 ? Gender.MALE : Gender.FEMALE,
                photoField.getText(),
                password,
                directionField.getEditor().getItem().toString());
        JOptionPane.showMessageDialog(this, "Модератор зарегистрирован: " + moderator.getIdNumber(), "Готово", JOptionPane.INFORMATION_MESSAGE);
        dispose();
    }
}
