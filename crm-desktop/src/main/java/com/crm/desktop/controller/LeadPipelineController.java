package com.crm.desktop.controller;

import com.crm.desktop.api.LeadApi;
import com.crm.desktop.service.SessionManager;
import com.crm.desktop.util.AlertHelper;
import com.crm.desktop.util.Formatter;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Controller for the Lead Pipeline (Kanban) screen.
 * Displays leads grouped by stage in a horizontal board.
 */
public class LeadPipelineController {

    @FXML private HBox kanbanBoard;
    @FXML private ProgressIndicator loadingSpinner;

    private final LeadApi leadApi = new LeadApi();
    private static final String[] STAGES = {
            "NEW", "CONTACTED", "QUALIFIED", "PROPOSAL", "WON", "LOST"
    };

    @FXML
    public void initialize() {
        loadLeads();
    }

    @FXML
    private void handleRefresh() {
        loadLeads();
    }

    private void loadLeads() {
        loadingSpinner.setVisible(true);
        SessionManager.getInstance().recordActivity();

        new Thread(() -> {
            try {
                // Load all leads (we'll group client-side)
                JsonObject response = leadApi.getLeads(0, 200, null);

                Platform.runLater(() -> {
                    loadingSpinner.setVisible(false);
                    kanbanBoard.getChildren().clear();

                    JsonArray allLeads = new JsonArray();
                    if (response.has("data")) {
                        JsonObject data = response.getAsJsonObject("data");
                        if (data.has("content")) {
                            allLeads = data.getAsJsonArray("content");
                        }
                    }

                    for (String stage : STAGES) {
                        VBox column = createStageColumn(stage, allLeads);
                        kanbanBoard.getChildren().add(column);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    loadingSpinner.setVisible(false);
                    AlertHelper.showNetworkError();
                });
            }
        }).start();
    }

    private VBox createStageColumn(String stage, JsonArray allLeads) {
        VBox column = new VBox(8);
        column.setPadding(new Insets(10));
        column.setMinWidth(200);
        column.setPrefWidth(200);
        column.setStyle("-fx-background-color: #f5f5f5; -fx-background-radius: 8;");

        Label header = new Label(stage);
        header.setStyle("-fx-font-size: 14; -fx-font-weight: bold;");
        column.getChildren().add(header);

        int count = 0;
        for (JsonElement el : allLeads) {
            JsonObject lead = el.getAsJsonObject();
            String leadStage = lead.has("stage") ? lead.get("stage").getAsString() : "";
            if (stage.equals(leadStage)) {
                VBox card = createLeadCard(lead);
                column.getChildren().add(card);
                count++;
            }
        }

        Label countLabel = new Label(count + " lead" + (count != 1 ? "s" : ""));
        countLabel.setStyle("-fx-text-fill: #888;");
        column.getChildren().add(1, countLabel);

        return column;
    }

    private VBox createLeadCard(JsonObject lead) {
        VBox card = new VBox(4);
        card.setPadding(new Insets(8));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 4; "
                + "-fx-border-color: #ddd; -fx-border-radius: 4;");

        String title = lead.has("title") ? lead.get("title").getAsString() : "";
        String value = lead.has("value") && !lead.get("value").isJsonNull()
                ? Formatter.formatCurrency(lead.get("value").getAsString()) : "";
        String customer = lead.has("customerName")
                ? lead.get("customerName").getAsString() : "";

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-weight: bold;");
        Label customerLabel = new Label(customer);
        customerLabel.setStyle("-fx-text-fill: #666;");
        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-text-fill: #2e7d32; -fx-font-weight: bold;");

        card.getChildren().addAll(titleLabel, customerLabel, valueLabel);
        return card;
    }
}
