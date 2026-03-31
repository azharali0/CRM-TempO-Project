package com.crm.desktop.controller;

import com.crm.desktop.api.CustomerApi;
import com.crm.desktop.service.SessionManager;
import com.crm.desktop.util.AlertHelper;
import com.crm.desktop.util.Validator;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;

/**
 * Controller for the Create/Edit Customer form.
 */
public class CustomerFormController {

    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private TextField companyField;
    @FXML private TextField addressField;
    @FXML private TextField cityField;
    @FXML private Button saveButton;
    @FXML private ProgressIndicator loadingSpinner;

    private final CustomerApi customerApi = new CustomerApi();
    private String editingCustomerId;
    private int editingVersion;

    @FXML
    public void initialize() {
        loadingSpinner.setVisible(false);
    }

    /**
     * Pre-populate the form for editing an existing customer.
     */
    public void setEditData(String id, String name, String email, String phone,
                            String company, String address, String city, int version) {
        this.editingCustomerId = id;
        this.editingVersion = version;
        nameField.setText(name);
        emailField.setText(email);
        phoneField.setText(phone);
        companyField.setText(company);
        addressField.setText(address);
        cityField.setText(city);
    }

    @FXML
    private void handleSave() {
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();

        if (!Validator.isValidName(name)) {
            AlertHelper.showError("Please enter a valid name.");
            return;
        }
        if (!email.isBlank() && !Validator.isValidEmail(email)) {
            AlertHelper.showError("Please enter a valid email address.");
            return;
        }

        loadingSpinner.setVisible(true);
        saveButton.setDisable(true);
        SessionManager.getInstance().recordActivity();

        new Thread(() -> {
            try {
                JsonObject data = new JsonObject();
                data.addProperty("name", name);
                if (!email.isBlank()) data.addProperty("email", email);
                if (!phone.isBlank()) data.addProperty("phone", phone);
                if (!companyField.getText().isBlank())
                    data.addProperty("company", companyField.getText().trim());
                if (!addressField.getText().isBlank())
                    data.addProperty("address", addressField.getText().trim());
                if (!cityField.getText().isBlank())
                    data.addProperty("city", cityField.getText().trim());

                JsonObject response;
                if (editingCustomerId != null) {
                    data.addProperty("version", editingVersion);
                    response = customerApi.updateCustomer(editingCustomerId, data);
                } else {
                    response = customerApi.createCustomer(data);
                }

                Platform.runLater(() -> {
                    loadingSpinner.setVisible(false);
                    saveButton.setDisable(false);
                    if (response.has("success") && response.get("success").getAsBoolean()) {
                        AlertHelper.showSuccess("Customer saved successfully.");
                    } else {
                        String msg = response.has("message")
                                ? response.get("message").getAsString()
                                : "Failed to save customer.";
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
