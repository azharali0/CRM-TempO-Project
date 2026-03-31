package com.crm.desktop.controller;

import com.crm.desktop.api.TaskApi;
import com.crm.desktop.service.SessionManager;
import com.crm.desktop.util.AlertHelper;
import com.crm.desktop.util.Validator;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

/**
 * Controller for the Task create/edit form.
 * Allows creating new tasks or editing existing ones.
 */
public class TaskFormController {

    @FXML private TextField titleField;
    @FXML private TextArea descriptionArea;
    @FXML private DatePicker dueDatePicker;
    @FXML private ComboBox<String> priorityCombo;
    @FXML private TextField assignedToField;
    @FXML private TextField customerIdField;
    @FXML private Button saveButton;
    @FXML private ProgressIndicator loadingSpinner;

    private final TaskApi taskApi = new TaskApi();
    private String editingTaskId;
    private int editingVersion;

    @FXML
    public void initialize() {
        loadingSpinner.setVisible(false);
        priorityCombo.setItems(FXCollections.observableArrayList(
                "LOW", "MEDIUM", "HIGH", "URGENT"));
        priorityCombo.getSelectionModel().select("MEDIUM");
    }

    /**
     * Pre-populate the form for editing an existing task.
     */
    public void setEditData(String id, String title, String description,
                            String dueDate, String priority, String assignedTo,
                            String customerId, int version) {
        this.editingTaskId = id;
        this.editingVersion = version;
        titleField.setText(title);
        descriptionArea.setText(description);
        if (dueDate != null && !dueDate.isBlank()) {
            try {
                dueDatePicker.setValue(java.time.LocalDate.parse(dueDate.substring(0, 10)));
            } catch (Exception ignored) {}
        }
        priorityCombo.getSelectionModel().select(priority);
        assignedToField.setText(assignedTo);
        customerIdField.setText(customerId);
    }

    @FXML
    private void handleSave() {
        String title = titleField.getText().trim();
        String description = descriptionArea.getText() != null ? descriptionArea.getText().trim() : "";

        if (!Validator.isValidName(title)) {
            AlertHelper.showError("Please enter a valid task title.");
            return;
        }
        if (dueDatePicker.getValue() == null) {
            AlertHelper.showError("Please select a due date.");
            return;
        }

        loadingSpinner.setVisible(true);
        saveButton.setDisable(true);
        SessionManager.getInstance().recordActivity();

        new Thread(() -> {
            try {
                JsonObject data = new JsonObject();
                data.addProperty("title", title);
                if (!description.isBlank()) {
                    data.addProperty("description", description);
                }
                data.addProperty("dueDate", dueDatePicker.getValue().toString());
                data.addProperty("priority", priorityCombo.getValue());

                String assignedTo = assignedToField.getText().trim();
                if (!assignedTo.isBlank()) {
                    data.addProperty("assignedToId", assignedTo);
                }
                String customerId = customerIdField.getText().trim();
                if (!customerId.isBlank()) {
                    data.addProperty("customerId", customerId);
                }

                JsonObject response;
                if (editingTaskId != null) {
                    data.addProperty("version", editingVersion);
                    response = taskApi.updateTask(editingTaskId, data);
                } else {
                    response = taskApi.createTask(data);
                }

                Platform.runLater(() -> {
                    loadingSpinner.setVisible(false);
                    saveButton.setDisable(false);
                    if (response.has("success") && response.get("success").getAsBoolean()) {
                        AlertHelper.showSuccess("Task saved successfully.");
                    } else {
                        String msg = response.has("message")
                                ? response.get("message").getAsString()
                                : "Failed to save task.";
                        AlertHelper.showError(msg);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    loadingSpinner.setVisible(false);
                    saveButton.setDisable(false);
                    AlertHelper.showNetworkError();
                });
            }
        }).start();
    }
}
