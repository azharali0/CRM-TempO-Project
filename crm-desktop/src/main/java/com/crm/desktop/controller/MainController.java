package com.crm.desktop.controller;

import com.crm.desktop.CrmDesktopApp;
import com.crm.desktop.api.AuthApi;
import com.crm.desktop.service.NotificationPoller;
import com.crm.desktop.service.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

/**
 * Controller for the Main application window.
 * Manages sidebar navigation and the content area.
 */
public class MainController {

    @FXML private BorderPane rootPane;
    @FXML private VBox sidebar;
    @FXML private Label userNameLabel;
    @FXML private Label userRoleLabel;
    @FXML private Label notificationBadge;
    @FXML private Button logoutButton;

    private final NotificationPoller notificationPoller = new NotificationPoller();

    @FXML
    public void initialize() {
        SessionManager session = SessionManager.getInstance();
        userNameLabel.setText(session.getUserName());
        userRoleLabel.setText(session.getUserRole());
        notificationBadge.setVisible(false);

        // Start polling for notifications
        notificationPoller.setOnUnreadCountChanged(count -> {
            if (count > 0) {
                notificationBadge.setText(String.valueOf(count));
                notificationBadge.setVisible(true);
            } else {
                notificationBadge.setVisible(false);
            }
        });
        notificationPoller.start();

        // Load dashboard by default
        loadScreen("/fxml/dashboard.fxml");
    }

    @FXML
    private void showCustomers() {
        loadScreen("/fxml/customer-list.fxml");
    }

    @FXML
    private void showLeads() {
        loadScreen("/fxml/lead-pipeline.fxml");
    }

    @FXML
    private void showTasks() {
        loadScreen("/fxml/task-list.fxml");
    }

    @FXML
    private void showDashboard() {
        loadScreen("/fxml/dashboard.fxml");
    }

    @FXML
    private void showReports() {
        loadScreen("/fxml/report.fxml");
    }

    @FXML
    private void handleLogout() {
        notificationPoller.stop();

        new Thread(() -> {
            try {
                String refreshToken = SessionManager.getInstance().getRefreshToken();
                if (refreshToken != null) {
                    new AuthApi().logout(refreshToken);
                }
            } catch (Exception ignored) {
                // Logout should always succeed client-side
            }
        }).start();

        SessionManager.getInstance().clearSession();
        CrmDesktopApp.showLoginScreen();
    }

    private void loadScreen(String fxmlPath) {
        try {
            SessionManager.getInstance().recordActivity();
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent screen = loader.load();
            rootPane.setCenter(screen);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
