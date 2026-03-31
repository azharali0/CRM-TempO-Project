package com.crm.desktop.controller;

import com.crm.desktop.api.ReportApi;
import com.crm.desktop.service.SessionManager;
import com.crm.desktop.util.Formatter;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;

/**
 * Controller for the Dashboard screen.
 * Displays KPI cards with real-time data.
 */
public class DashboardController {

    @FXML private Label totalCustomersLabel;
    @FXML private Label activeLeadsLabel;
    @FXML private Label pendingTasksLabel;
    @FXML private Label wonDealsLabel;
    @FXML private Label conversionRateLabel;
    @FXML private Label revenueLabel;
    @FXML private Label welcomeLabel;
    @FXML private ProgressIndicator loadingSpinner;

    private final ReportApi reportApi = new ReportApi();

    @FXML
    public void initialize() {
        SessionManager session = SessionManager.getInstance();
        welcomeLabel.setText("Welcome, " + session.getUserName());
        loadDashboard();
    }

    @FXML
    private void handleRefresh() {
        loadDashboard();
    }

    private void loadDashboard() {
        loadingSpinner.setVisible(true);
        SessionManager.getInstance().recordActivity();

        new Thread(() -> {
            try {
                JsonObject response = reportApi.getDashboard();

                Platform.runLater(() -> {
                    loadingSpinner.setVisible(false);
                    if (response.has("data")) {
                        JsonObject data = response.getAsJsonObject("data");
                        totalCustomersLabel.setText(
                                String.valueOf(getInt(data, "totalCustomers")));
                        activeLeadsLabel.setText(
                                String.valueOf(getInt(data, "activeLeads")));
                        pendingTasksLabel.setText(
                                String.valueOf(getInt(data, "pendingTasks")));
                        wonDealsLabel.setText(
                                String.valueOf(getInt(data, "wonDeals")));

                        if (data.has("conversionRate")) {
                            double rate = data.get("conversionRate").getAsDouble();
                            conversionRateLabel.setText(
                                    String.format("%.1f%%", rate));
                        }
                        if (data.has("totalRevenue") && !data.get("totalRevenue").isJsonNull()) {
                            revenueLabel.setText(
                                    Formatter.formatCurrency(
                                            data.get("totalRevenue").getAsString()));
                        }
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> loadingSpinner.setVisible(false));
            }
        }).start();
    }

    private int getInt(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull()
                ? obj.get(key).getAsInt() : 0;
    }
}
