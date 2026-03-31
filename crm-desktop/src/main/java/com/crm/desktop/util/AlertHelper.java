package com.crm.desktop.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.util.Optional;

/**
 * Reusable JavaFX confirmation, error, and success dialogs.
 */
public final class AlertHelper {

    private AlertHelper() {}

    /**
     * Shows an info alert.
     */
    public static void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Shows a success alert.
     */
    public static void showSuccess(String message) {
        showInfo("Success", message);
    }

    /**
     * Shows an error alert.
     */
    public static void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Shows an error alert with a default title.
     */
    public static void showError(String message) {
        showError("Error", message);
    }

    /**
     * Shows a warning alert.
     */
    public static void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Shows a confirmation dialog. Returns true if user clicks OK.
     */
    public static boolean showConfirmation(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    /**
     * Shows a network error message.
     */
    public static void showNetworkError() {
        showError("Connection Error",
                "Cannot connect to server. Please check your network connection.");
    }
}
