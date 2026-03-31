package com.crm.desktop.controller;

import com.crm.desktop.api.InteractionApi;
import com.crm.desktop.service.SessionManager;
import com.crm.desktop.util.AlertHelper;
import com.crm.desktop.util.Formatter;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

/**
 * Controller for the Interaction Timeline screen.
 * Displays a customer's interaction history in chronological order
 * and allows logging new interactions (CALL, EMAIL, MEETING, NOTE).
 */
public class InteractionTimelineController {

    @FXML private TableView<JsonObject> timelineTable;
    @FXML private TableColumn<JsonObject, String> dateColumn;
    @FXML private TableColumn<JsonObject, String> typeColumn;
    @FXML private TableColumn<JsonObject, String> subjectColumn;
    @FXML private TableColumn<JsonObject, String> notesColumn;
    @FXML private TableColumn<JsonObject, String> loggedByColumn;
    @FXML private ComboBox<String> typeFilter;
    @FXML private Label pageLabel;
    @FXML private ProgressIndicator loadingSpinner;

    // Fields for logging a new interaction
    @FXML private ComboBox<String> newTypeCombo;
    @FXML private TextField newSubjectField;
    @FXML private TextArea newNotesArea;
    @FXML private Spinner<Integer> durationSpinner;

    private final InteractionApi interactionApi = new InteractionApi();
    private String customerId;
    private int currentPage = 0;
    private int totalPages = 0;

    @FXML
    public void initialize() {
        dateColumn.setCellValueFactory(d ->
                new SimpleStringProperty(
                        Formatter.formatDate(getStr(d.getValue(), "createdAt"))));
        typeColumn.setCellValueFactory(d ->
                new SimpleStringProperty(getStr(d.getValue(), "type")));
        subjectColumn.setCellValueFactory(d ->
                new SimpleStringProperty(getStr(d.getValue(), "subject")));
        notesColumn.setCellValueFactory(d ->
                new SimpleStringProperty(getStr(d.getValue(), "notes")));
        loggedByColumn.setCellValueFactory(d ->
                new SimpleStringProperty(getStr(d.getValue(), "loggedByName")));

        typeFilter.setItems(FXCollections.observableArrayList(
                "ALL", "CALL", "EMAIL", "MEETING", "NOTE"));
        typeFilter.getSelectionModel().selectFirst();

        newTypeCombo.setItems(FXCollections.observableArrayList(
                "CALL", "EMAIL", "MEETING", "NOTE"));
        newTypeCombo.getSelectionModel().selectFirst();

        durationSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 480, 15, 5));
    }

    /**
     * Sets the customer whose interactions to display.
     */
    public void setCustomerId(String id) {
        this.customerId = id;
        loadTimeline();
    }

    @FXML
    private void handleFilter() {
        currentPage = 0;
        loadTimeline();
    }

    @FXML
    private void handlePrevPage() {
        if (currentPage > 0) { currentPage--; loadTimeline(); }
    }

    @FXML
    private void handleNextPage() {
        if (currentPage < totalPages - 1) { currentPage++; loadTimeline(); }
    }

    @FXML
    private void handleLogInteraction() {
        if (customerId == null || customerId.isBlank()) {
            AlertHelper.showError("No customer selected.");
            return;
        }
        String subject = newSubjectField.getText().trim();
        if (subject.isBlank()) {
            AlertHelper.showError("Please enter a subject.");
            return;
        }

        loadingSpinner.setVisible(true);
        SessionManager.getInstance().recordActivity();

        new Thread(() -> {
            try {
                JsonObject data = new JsonObject();
                data.addProperty("customerId", customerId);
                data.addProperty("type", newTypeCombo.getValue());
                data.addProperty("subject", subject);
                String notes = newNotesArea.getText() != null ? newNotesArea.getText().trim() : "";
                if (!notes.isBlank()) {
                    data.addProperty("notes", notes);
                }
                data.addProperty("duration", durationSpinner.getValue());

                JsonObject response = interactionApi.createInteraction(data);

                Platform.runLater(() -> {
                    loadingSpinner.setVisible(false);
                    if (response.has("success") && response.get("success").getAsBoolean()) {
                        AlertHelper.showSuccess("Interaction logged successfully.");
                        newSubjectField.clear();
                        newNotesArea.clear();
                        loadTimeline();
                    } else {
                        AlertHelper.showError(response.has("message")
                                ? response.get("message").getAsString()
                                : "Failed to log interaction.");
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

    private void loadTimeline() {
        if (customerId == null) return;
        loadingSpinner.setVisible(true);
        SessionManager.getInstance().recordActivity();

        new Thread(() -> {
            try {
                JsonObject response = interactionApi.getCustomerInteractions(
                        customerId, currentPage, 20);

                Platform.runLater(() -> {
                    loadingSpinner.setVisible(false);
                    if (response.has("data")) {
                        JsonObject data = response.getAsJsonObject("data");
                        JsonArray content = data.has("content")
                                ? data.getAsJsonArray("content") : new JsonArray();
                        totalPages = data.has("totalPages")
                                ? data.get("totalPages").getAsInt() : 1;

                        ObservableList<JsonObject> items = FXCollections.observableArrayList();
                        String filter = typeFilter.getValue();
                        for (JsonElement el : content) {
                            JsonObject item = el.getAsJsonObject();
                            if ("ALL".equals(filter) || filter.equals(getStr(item, "type"))) {
                                items.add(item);
                            }
                        }
                        timelineTable.setItems(items);
                        pageLabel.setText("Page " + (currentPage + 1) + " of " + totalPages);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> loadingSpinner.setVisible(false));
            }
        }).start();
    }

    private String getStr(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull()
                ? obj.get(key).getAsString() : "";
    }
}
