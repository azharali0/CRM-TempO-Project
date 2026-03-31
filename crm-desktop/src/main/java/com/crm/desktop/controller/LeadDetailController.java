package com.crm.desktop.controller;

import com.crm.desktop.api.InteractionApi;
import com.crm.desktop.api.LeadApi;
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
 * Controller for the Lead detail screen.
 * Shows lead profile, current stage, value, and related interactions.
 */
public class LeadDetailController {

    @FXML private Label titleLabel;
    @FXML private Label stageLabel;
    @FXML private Label valueLabel;
    @FXML private Label probabilityLabel;
    @FXML private Label customerLabel;
    @FXML private Label ownerLabel;
    @FXML private Label expectedCloseLabel;
    @FXML private Label lostReasonLabel;
    @FXML private Label createdAtLabel;
    @FXML private ListView<String> interactionList;
    @FXML private ProgressIndicator loadingSpinner;

    private final LeadApi leadApi = new LeadApi();
    private final InteractionApi interactionApi = new InteractionApi();
    private String leadId;
    private String customerId;

    /**
     * Sets the lead ID and loads detail data.
     */
    public void setLeadId(String id) {
        this.leadId = id;
        loadLeadDetail();
    }

    private void loadLeadDetail() {
        loadingSpinner.setVisible(true);
        SessionManager.getInstance().recordActivity();

        new Thread(() -> {
            try {
                JsonObject response = leadApi.getLeadById(leadId);

                Platform.runLater(() -> {
                    loadingSpinner.setVisible(false);
                    if (response.has("data")) {
                        JsonObject data = response.getAsJsonObject("data");
                        titleLabel.setText(getStr(data, "title"));
                        stageLabel.setText(getStr(data, "stage"));

                        String value = data.has("value") && !data.get("value").isJsonNull()
                                ? Formatter.formatCurrency(data.get("value").getAsString()) : "$0.00";
                        valueLabel.setText(value);

                        probabilityLabel.setText(
                                data.has("probability") ? data.get("probability").getAsString() + "%" : "—");
                        customerLabel.setText(getStr(data, "customerName"));
                        ownerLabel.setText(getStr(data, "ownerName"));
                        expectedCloseLabel.setText(
                                Formatter.formatDate(getStr(data, "expectedCloseDate")));

                        String lostReason = getStr(data, "lostReason");
                        lostReasonLabel.setText(lostReason.isEmpty() ? "—" : lostReason);
                        lostReasonLabel.setVisible(!"—".equals(lostReasonLabel.getText()));

                        createdAtLabel.setText(Formatter.formatDate(getStr(data, "createdAt")));

                        // Load interactions for the customer associated with this lead
                        customerId = getStr(data, "customerId");
                        if (!customerId.isEmpty()) {
                            loadInteractions();
                        }
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    loadingSpinner.setVisible(false);
                    AlertHelper.showError("Failed to load lead details.");
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
                        interactionList.getItems().clear();
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
