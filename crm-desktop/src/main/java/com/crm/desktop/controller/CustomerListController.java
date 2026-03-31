package com.crm.desktop.controller;

import com.crm.desktop.api.CustomerApi;
import com.crm.desktop.service.SessionManager;
import com.crm.desktop.util.Formatter;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;

/**
 * Controller for the Customer list screen.
 * Displays paginated, filterable customer table.
 */
public class CustomerListController {

    @FXML private TableView<JsonObject> customerTable;
    @FXML private TableColumn<JsonObject, String> nameColumn;
    @FXML private TableColumn<JsonObject, String> emailColumn;
    @FXML private TableColumn<JsonObject, String> phoneColumn;
    @FXML private TableColumn<JsonObject, String> companyColumn;
    @FXML private TableColumn<JsonObject, String> statusColumn;
    @FXML private TextField searchField;
    @FXML private Label pageLabel;
    @FXML private ProgressIndicator loadingSpinner;

    private final CustomerApi customerApi = new CustomerApi();
    private int currentPage = 0;
    private int totalPages = 0;

    @FXML
    public void initialize() {
        nameColumn.setCellValueFactory(d ->
                new SimpleStringProperty(getStr(d.getValue(), "name")));
        emailColumn.setCellValueFactory(d ->
                new SimpleStringProperty(getStr(d.getValue(), "email")));
        phoneColumn.setCellValueFactory(d ->
                new SimpleStringProperty(getStr(d.getValue(), "maskedPhone")));
        companyColumn.setCellValueFactory(d ->
                new SimpleStringProperty(getStr(d.getValue(), "company")));
        statusColumn.setCellValueFactory(d ->
                new SimpleStringProperty(getStr(d.getValue(), "status")));

        customerTable.setRowFactory(tv -> {
            TableRow<JsonObject> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    openCustomerDetail(row.getItem());
                }
            });
            return row;
        });

        loadCustomers();
    }

    @FXML
    private void handleSearch() {
        currentPage = 0;
        loadCustomers();
    }

    @FXML
    private void handlePrevPage() {
        if (currentPage > 0) {
            currentPage--;
            loadCustomers();
        }
    }

    @FXML
    private void handleNextPage() {
        if (currentPage < totalPages - 1) {
            currentPage++;
            loadCustomers();
        }
    }

    @FXML
    private void handleAddCustomer() {
        loadInParent("/fxml/customer-form.fxml");
    }

    private void loadCustomers() {
        loadingSpinner.setVisible(true);
        SessionManager.getInstance().recordActivity();

        new Thread(() -> {
            try {
                String search = searchField != null ? searchField.getText() : null;
                JsonObject response = customerApi.getCustomers(
                        currentPage, 20, search, null, null, null);

                Platform.runLater(() -> {
                    loadingSpinner.setVisible(false);
                    if (response.has("data")) {
                        JsonObject data = response.getAsJsonObject("data");
                        JsonArray content = data.has("content")
                                ? data.getAsJsonArray("content")
                                : new JsonArray();
                        totalPages = data.has("totalPages")
                                ? data.get("totalPages").getAsInt() : 1;

                        ObservableList<JsonObject> items = FXCollections.observableArrayList();
                        for (JsonElement el : content) {
                            items.add(el.getAsJsonObject());
                        }
                        customerTable.setItems(items);
                        pageLabel.setText("Page " + (currentPage + 1) + " of " + totalPages);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> loadingSpinner.setVisible(false));
            }
        }).start();
    }

    private void openCustomerDetail(JsonObject customer) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/customer-detail.fxml"));
            Parent detail = loader.load();
            CustomerDetailController controller = loader.getController();
            controller.setCustomerId(getStr(customer, "id"));

            BorderPane parent = (BorderPane) customerTable.getScene().lookup("#rootPane");
            if (parent != null) {
                parent.setCenter(detail);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadInParent(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent screen = loader.load();
            BorderPane parent = (BorderPane) customerTable.getScene().lookup("#rootPane");
            if (parent != null) {
                parent.setCenter(screen);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getStr(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull()
                ? obj.get(key).getAsString() : "";
    }
}
