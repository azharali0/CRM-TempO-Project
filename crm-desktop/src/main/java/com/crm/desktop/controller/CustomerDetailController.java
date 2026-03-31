package com.crm.desktop.controller;

import com.crm.desktop.api.CustomerApi;
import com.crm.desktop.api.InteractionApi;
import com.crm.desktop.service.SessionManager;
import com.crm.desktop.util.AlertHelper;
import com.crm.desktop.util.Formatter;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;

/**
 * Controller for the Customer detail screen.
 * Shows full customer profile + interaction timeline.
 */
public class CustomerDetailController {

    @FXML private Label nameLabel;
    @FXML private Label emailLabel;
    @FXML private Label phoneLabel;
    @FXML private Label companyLabel;
    @FXML private Label cityLabel;
    @FXML private Label statusLabel;
    @FXML private Label assignedToLabel;
    @FXML private ListView<String> interactionList;
    @FXML private ProgressIndicator loadingSpinner;

    private final CustomerApi customerApi = new CustomerApi();
    private final InteractionApi interactionApi = new InteractionApi();
    private String customerId;

    public void setCustomerId(String id) {
        this.customerId = id;
        loadCustomerDetail();
        loadInteractions();
    }

    private void loadCustomerDetail() {
        loadingSpinner.setVisible(true);
        SessionManager.getInstance().recordActivity();

        new Thread(() -> {
            try {
                JsonObject response = customerApi.getCustomerById(customerId);
                Platform.runLater(() -> {
                    loadingSpinner.setVisible(false);
                    if (response.has("data")) {
                        JsonObject data = response.getAsJsonObject("data");
                        nameLabel.setText(getStr(data, "name"));
                        emailLabel.setText(getStr(data, "email"));
                        phoneLabel.setText(getStr(data, "phone"));
                        companyLabel.setText(getStr(data, "company"));
                        cityLabel.setText(getStr(data, "city"));
                        statusLabel.setText(getStr(data, "status"));
                        assignedToLabel.setText(getStr(data, "assignedToName"));
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    loadingSpinner.setVisible(false);
                    AlertHelper.showError("Failed to load customer details.");
                });
            }
        }).start();
    }

    private void loadInteractions() {
        new Thread(() -> {
            try {
                JsonObject response = interactionApi.getCustomerInteractions(
                        customerId, 0, 50);
                Platform.runLater(() -> {
                    if (response.has("data")) {
                        JsonObject data = response.getAsJsonObject("data");
                        JsonArray content = data.has("content")
                                ? data.getAsJsonArray("content") : new JsonArray();
                        for (JsonElement el : content) {
                            JsonObject interaction = el.getAsJsonObject();
                            String line = String.format("[%s] %s — %s",
                                    getStr(interaction, "type"),
                                    getStr(interaction, "subject"),
                                    Formatter.formatDate(getStr(interaction, "createdAt")));
                            interactionList.getItems().add(line);
                        }
                    }
                });
            } catch (Exception ignored) {}
        }).start();
    }

    private String getStr(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull()
                ? obj.get(key).getAsString() : "";
    }
}
