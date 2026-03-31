package com.crm.desktop.controller;

import com.crm.desktop.api.ReportApi;
import com.crm.desktop.service.SessionManager;
import com.crm.desktop.util.AlertHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileOutputStream;

/**
 * Controller for the Reports screen.
 * Shows conversion rates, sales by rep, monthly trends, PDF export.
 */
public class ReportController {

    @FXML private TableView<JsonObject> salesRepTable;
    @FXML private TableColumn<JsonObject, String> repNameColumn;
    @FXML private TableColumn<JsonObject, String> repDealsColumn;
    @FXML private TableColumn<JsonObject, String> repRevenueColumn;
    @FXML private Label conversionLabel;
    @FXML private ProgressIndicator loadingSpinner;

    private final ReportApi reportApi = new ReportApi();

    @FXML
    public void initialize() {
        repNameColumn.setCellValueFactory(d ->
                new SimpleStringProperty(getStr(d.getValue(), "repName")));
        repDealsColumn.setCellValueFactory(d ->
                new SimpleStringProperty(getStr(d.getValue(), "wonDeals")));
        repRevenueColumn.setCellValueFactory(d ->
                new SimpleStringProperty(getStr(d.getValue(), "totalRevenue")));

        loadReports();
    }

    @FXML
    private void handleRefresh() {
        loadReports();
    }

    @FXML
    private void handleExportPdf() {
        SessionManager.getInstance().recordActivity();

        new Thread(() -> {
            try {
                byte[] pdfBytes = reportApi.downloadPdf("customers", null);
                Platform.runLater(() -> {
                    FileChooser fileChooser = new FileChooser();
                    fileChooser.setTitle("Save PDF Report");
                    fileChooser.getExtensionFilters().add(
                            new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
                    fileChooser.setInitialFileName("crm-report.pdf");
                    File file = fileChooser.showSaveDialog(null);
                    if (file != null) {
                        try (FileOutputStream fos = new FileOutputStream(file)) {
                            fos.write(pdfBytes);
                            AlertHelper.showSuccess("PDF saved to " + file.getName());
                        } catch (Exception e) {
                            AlertHelper.showError("Failed to save PDF.");
                        }
                    }
                });
            } catch (Exception e) {
                Platform.runLater(AlertHelper::showNetworkError);
            }
        }).start();
    }

    private void loadReports() {
        loadingSpinner.setVisible(true);
        SessionManager.getInstance().recordActivity();

        new Thread(() -> {
            try {
                JsonObject conversionResponse = reportApi.getConversionReport();
                JsonObject salesResponse = reportApi.getSalesByRep();

                Platform.runLater(() -> {
                    loadingSpinner.setVisible(false);

                    // Conversion
                    if (conversionResponse.has("data")) {
                        JsonObject data = conversionResponse.getAsJsonObject("data");
                        if (data.has("conversionRate")) {
                            conversionLabel.setText(String.format("%.1f%%",
                                    data.get("conversionRate").getAsDouble()));
                        }
                    }

                    // Sales by rep
                    if (salesResponse.has("data")) {
                        JsonElement dataEl = salesResponse.get("data");
                        if (dataEl.isJsonArray()) {
                            JsonArray arr = dataEl.getAsJsonArray();
                            ObservableList<JsonObject> items =
                                    FXCollections.observableArrayList();
                            for (JsonElement el : arr) {
                                items.add(el.getAsJsonObject());
                            }
                            salesRepTable.setItems(items);
                        }
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
