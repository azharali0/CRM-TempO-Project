package com.crm.desktop.controller;

import com.crm.desktop.api.TaskApi;
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
 * Controller for the Task list screen.
 * Shows paginated task table with status filtering.
 */
public class TaskListController {

    @FXML private TableView<JsonObject> taskTable;
    @FXML private TableColumn<JsonObject, String> titleColumn;
    @FXML private TableColumn<JsonObject, String> dueDateColumn;
    @FXML private TableColumn<JsonObject, String> priorityColumn;
    @FXML private TableColumn<JsonObject, String> statusColumn;
    @FXML private TableColumn<JsonObject, String> assignedToColumn;
    @FXML private ComboBox<String> statusFilter;
    @FXML private Label pageLabel;
    @FXML private ProgressIndicator loadingSpinner;

    private final TaskApi taskApi = new TaskApi();
    private int currentPage = 0;
    private int totalPages = 0;

    @FXML
    public void initialize() {
        titleColumn.setCellValueFactory(d ->
                new SimpleStringProperty(getStr(d.getValue(), "title")));
        dueDateColumn.setCellValueFactory(d ->
                new SimpleStringProperty(
                        Formatter.formatDate(getStr(d.getValue(), "dueDate"))));
        priorityColumn.setCellValueFactory(d ->
                new SimpleStringProperty(getStr(d.getValue(), "priority")));
        statusColumn.setCellValueFactory(d ->
                new SimpleStringProperty(getStr(d.getValue(), "status")));
        assignedToColumn.setCellValueFactory(d ->
                new SimpleStringProperty(getStr(d.getValue(), "assignedToName")));

        statusFilter.setItems(FXCollections.observableArrayList(
                "ALL", "PENDING", "IN_PROGRESS", "DONE", "OVERDUE"));
        statusFilter.getSelectionModel().selectFirst();

        loadTasks();
    }

    @FXML
    private void handleFilter() {
        currentPage = 0;
        loadTasks();
    }

    @FXML
    private void handlePrevPage() {
        if (currentPage > 0) { currentPage--; loadTasks(); }
    }

    @FXML
    private void handleNextPage() {
        if (currentPage < totalPages - 1) { currentPage++; loadTasks(); }
    }

    @FXML
    private void handleComplete() {
        JsonObject selected = taskTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertHelper.showWarning("No Selection", "Please select a task to complete.");
            return;
        }
        String taskId = getStr(selected, "id");
        new Thread(() -> {
            try {
                JsonObject response = taskApi.completeTask(taskId);
                Platform.runLater(() -> {
                    if (response.has("success") && response.get("success").getAsBoolean()) {
                        AlertHelper.showSuccess("Task marked as complete.");
                        loadTasks();
                    } else {
                        AlertHelper.showError(response.has("message")
                                ? response.get("message").getAsString()
                                : "Failed to complete task.");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(AlertHelper::showNetworkError);
            }
        }).start();
    }

    private void loadTasks() {
        loadingSpinner.setVisible(true);
        SessionManager.getInstance().recordActivity();

        new Thread(() -> {
            try {
                String filter = statusFilter.getValue();
                String statusParam = "ALL".equals(filter) ? null : filter;

                JsonObject response;
                if (SessionManager.getInstance().isSalesRep()) {
                    response = taskApi.getMyTasks(currentPage, 20, statusParam);
                } else {
                    response = taskApi.getTasks(currentPage, 20, statusParam);
                }

                Platform.runLater(() -> {
                    loadingSpinner.setVisible(false);
                    if (response.has("data")) {
                        JsonObject data = response.getAsJsonObject("data");
                        JsonArray content = data.has("content")
                                ? data.getAsJsonArray("content") : new JsonArray();
                        totalPages = data.has("totalPages")
                                ? data.get("totalPages").getAsInt() : 1;

                        ObservableList<JsonObject> items = FXCollections.observableArrayList();
                        for (JsonElement el : content) {
                            items.add(el.getAsJsonObject());
                        }
                        taskTable.setItems(items);
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
