package com.example.eventapp.ui;

import com.example.eventapp.model.AuthenticatedUser;
import com.example.eventapp.service.AppContext;
import com.example.eventapp.util.CaptchaGenerator;

import javax.swing.*;
import java.awt.*;
import java.util.Base64;
import java.util.Optional;
import java.util.prefs.Preferences;

public class LoginDialog extends JDialog {
    private static final int CAPTCHA_LENGTH = 4;

    private final AppContext context;
    private final JTextField idField = new JTextField(20);
    private final JPasswordField passwordField = new JPasswordField(20);
    private final JTextField captchaField = new JTextField(6);
    private final JCheckBox rememberCheck = new JCheckBox("Запомнить меня");
    private final JLabel captchaLabel = new JLabel();
    private final JButton loginButton = new JButton("Войти");
    private final Preferences preferences = Preferences.userNodeForPackage(LoginDialog.class);

    private String captchaValue;
    private int attempts;
    private Optional<AuthenticatedUser> authenticatedUser = Optional.empty();

    public LoginDialog(Frame owner, AppContext context) {
        super(owner, "Авторизация", true);
        this.context = context;
        initUi();
        loadRememberedCredentials();
        regenerateCaptcha();
    }

    private void initUi() {
        setSize(420, 280);
        setLocationRelativeTo(getOwner());
        setLayout(new BorderLayout(8, 8));

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0;
        gbc.gridy = 0;
        form.add(new JLabel("ID Number:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        form.add(idField, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        form.add(new JLabel("Пароль:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        form.add(passwordField, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        form.add(new JLabel("CAPTCHA:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        JPanel captchaPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        captchaPanel.add(captchaLabel);
        captchaPanel.add(captchaField);
        JButton refresh = new JButton("Обновить");
        refresh.addActionListener(e -> regenerateCaptcha());
        captchaPanel.add(refresh);
        form.add(captchaPanel, gbc);

        gbc.gridy++;
        gbc.gridx = 1;
        form.add(rememberCheck, gbc);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        loginButton.addActionListener(e -> authenticate());
        JButton cancelButton = new JButton("Отмена");
        cancelButton.addActionListener(e -> dispose());
        bottom.add(cancelButton);
        bottom.add(loginButton);

        add(form, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
    }

    private void loadRememberedCredentials() {
        String savedId = preferences.get("rememberedId", "");
        String savedPassword = preferences.get("rememberedPassword", "");
        if (!savedId.isBlank() && !savedPassword.isBlank()) {
            idField.setText(savedId);
            passwordField.setText(new String(Base64.getDecoder().decode(savedPassword)));
            rememberCheck.setSelected(true);
        }
    }

    private void regenerateCaptcha() {
        CaptchaGenerator.CaptchaImage captcha = CaptchaGenerator.generate(CAPTCHA_LENGTH, 150, 50);
        captchaValue = captcha.text();
        captchaLabel.setIcon(new ImageIcon(captcha.image()));
        captchaField.setText("");
    }

    private void authenticate() {
        if (idField.getText().isBlank() || passwordField.getPassword().length == 0) {
            JOptionPane.showMessageDialog(this, "Введите учетные данные", "Ошибка", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!captchaValue.equalsIgnoreCase(captchaField.getText().trim())) {
            JOptionPane.showMessageDialog(this, "CAPTCHA введена неверно", "Ошибка", JOptionPane.ERROR_MESSAGE);
            regenerateCaptcha();
            registerAttempt();
            return;
        }
        String password = new String(passwordField.getPassword());
        context.authenticationService().authenticate(idField.getText().trim(), password)
                .ifPresentOrElse(user -> {
                    authenticatedUser = Optional.of(user);
                    persistRememberMe(password);
                    dispose();
                }, () -> {
                    JOptionPane.showMessageDialog(this, "Неверные учетные данные", "Ошибка", JOptionPane.ERROR_MESSAGE);
                    regenerateCaptcha();
                    registerAttempt();
                });
    }

    private void persistRememberMe(String password) {
        if (rememberCheck.isSelected()) {
            preferences.put("rememberedId", idField.getText().trim());
            preferences.put("rememberedPassword", Base64.getEncoder().encodeToString(password.getBytes()));
        } else {
            preferences.remove("rememberedId");
            preferences.remove("rememberedPassword");
        }
    }

    private void registerAttempt() {
        attempts++;
        if (attempts >= 3) {
            loginButton.setEnabled(false);
            Timer timer = new Timer(10_000, e -> loginButton.setEnabled(true));
            timer.setRepeats(false);
            timer.start();
            attempts = 0;
        }
    }

    public Optional<AuthenticatedUser> getAuthenticatedUser() {
        return authenticatedUser;
    }
}
