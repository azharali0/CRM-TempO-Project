package com.crm.desktop.controller;

import com.crm.desktop.api.CustomerApi;
import com.crm.desktop.service.SessionManager;
import com.crm.desktop.util.AlertHelper;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Controller for the Excel/CSV Import dialog.
 * Allows importing customer data from .xlsx or .csv files.
 * Shows progress and import results (created/failed/errors).
 */
public class ImportController {

    @FXML private Label fileNameLabel;
    @FXML private Label statusLabel;
    @FXML private Label resultLabel;
    @FXML private Button selectFileButton;
    @FXML private Button importButton;
    @FXML private ProgressBar progressBar;
    @FXML private ProgressIndicator loadingSpinner;

    private final CustomerApi customerApi = new CustomerApi();
    private File selectedFile;

    @FXML
    public void initialize() {
        loadingSpinner.setVisible(false);
        progressBar.setVisible(false);
        importButton.setDisable(true);
        statusLabel.setText("Select an Excel (.xlsx) or CSV (.csv) file to import.");
        resultLabel.setText("");
    }

    @FXML
    private void handleSelectFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Import File");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Spreadsheet Files", "*.xlsx", "*.csv"),
                new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"),
                new FileChooser.ExtensionFilter("CSV Files", "*.csv"));

        File file = fileChooser.showOpenDialog(selectFileButton.getScene().getWindow());
        if (file != null) {
            // Validate file size (max 10MB)
            if (file.length() > 10 * 1024 * 1024) {
                AlertHelper.showError("File too large. Maximum size is 10MB.");
                return;
            }
            selectedFile = file;
            fileNameLabel.setText(file.getName());
            importButton.setDisable(false);
            statusLabel.setText("Ready to import: " + file.getName());
            resultLabel.setText("");
        }
    }

    @FXML
    private void handleImport() {
        if (selectedFile == null || !selectedFile.exists()) {
            AlertHelper.showError("No file selected.");
            return;
        }

        loadingSpinner.setVisible(true);
        progressBar.setVisible(true);
        progressBar.setProgress(-1); // indeterminate
        importButton.setDisable(true);
        selectFileButton.setDisable(true);
        statusLabel.setText("Importing... Please wait.");
        resultLabel.setText("");
        SessionManager.getInstance().recordActivity();

        new Thread(() -> {
            try {
                byte[] fileBytes = Files.readAllBytes(selectedFile.toPath());
                JsonObject response = customerApi.importCustomers(
                        fileBytes, selectedFile.getName());

                Platform.runLater(() -> {
                    loadingSpinner.setVisible(false);
                    progressBar.setVisible(false);
                    importButton.setDisable(false);
                    selectFileButton.setDisable(false);

                    if (response.has("success") && response.get("success").getAsBoolean()) {
                        statusLabel.setText("Import completed successfully!");
                        if (response.has("data")) {
                            JsonObject data = response.getAsJsonObject("data");
                            int created = data.has("created") ? data.get("created").getAsInt() : 0;
                            int failed = data.has("failed") ? data.get("failed").getAsInt() : 0;
                            resultLabel.setText(String.format(
                                    "Created: %d  |  Failed: %d", created, failed));
                        }
                        progressBar.setVisible(true);
                        progressBar.setProgress(1.0);
                    } else {
                        statusLabel.setText("Import failed.");
                        String msg = response.has("message")
                                ? response.get("message").getAsString()
                                : "Unknown error during import.";
                        resultLabel.setText(msg);
                    }
                });
            } catch (IOException e) {
                Platform.runLater(() -> {
                    loadingSpinner.setVisible(false);
                    progressBar.setVisible(false);
                    importButton.setDisable(false);
                    selectFileButton.setDisable(false);
                    statusLabel.setText("Failed to read file.");
                    AlertHelper.showError("Could not read the selected file: " + e.getMessage());
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    loadingSpinner.setVisible(false);
                    progressBar.setVisible(false);
                    importButton.setDisable(false);
                    selectFileButton.setDisable(false);
                    statusLabel.setText("Import failed — network error.");
                    AlertHelper.showNetworkError();
                });
            }
        }).start();
    }
}
