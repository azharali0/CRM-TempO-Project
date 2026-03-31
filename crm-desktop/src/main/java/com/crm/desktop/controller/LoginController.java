package com.crm.desktop.controller;

import com.crm.desktop.CrmDesktopApp;
import com.crm.desktop.api.AuthApi;
import com.crm.desktop.service.SessionManager;
import com.crm.desktop.util.AlertHelper;
import com.crm.desktop.util.Validator;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;

/**
 * Controller for the Login screen.
 * Sends POST /api/auth/login and stores tokens in SessionManager (memory only).
 */
public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Label errorLabel;
    @FXML private ProgressIndicator loadingSpinner;

    private final AuthApi authApi = new AuthApi();

    @FXML
    public void initialize() {
        errorLabel.setVisible(false);
        loadingSpinner.setVisible(false);
    }

    @FXML
    private void handleLogin() {
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        // Client-side validation
        if (Validator.isNullOrBlank(email) || Validator.isNullOrBlank(password)) {
            showError("Please enter both email and password.");
            return;
        }
        if (!Validator.isValidEmail(email)) {
            showError("Please enter a valid email address.");
            return;
        }

        setLoading(true);
        errorLabel.setVisible(false);

        // Run API call on a background thread to avoid freezing the UI
        new Thread(() -> {
            try {
                JsonObject response = authApi.login(email, password);

                Platform.runLater(() -> {
                    setLoading(false);

                    if (response.has("success") && response.get("success").getAsBoolean()) {
                        JsonObject data = response.getAsJsonObject("data");
                        SessionManager.getInstance().setSession(
                                data.get("accessToken").getAsString(),
                                data.get("refreshToken").getAsString(),
                                data.get("name").getAsString(),
                                data.get("email").getAsString(),
                                data.get("role").getAsString()
                        );
                        CrmDesktopApp.showMainWindow();
                    } else {
                        // Generic error — never reveal whether email exists
                        String msg = response.has("message")
                                ? response.get("message").getAsString()
                                : "Invalid credentials";
                        showError(msg);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    setLoading(false);
                    showError("Cannot connect to server. Please check your connection.");
                });
            }
        }).start();
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    private void setLoading(boolean loading) {
        loadingSpinner.setVisible(loading);
        loginButton.setDisable(loading);
        emailField.setDisable(loading);
        passwordField.setDisable(loading);
    }
}
