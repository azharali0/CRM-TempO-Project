package com.crm.desktop;

import com.crm.desktop.service.SessionManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Main entry point for the CRM Desktop Application.
 * Launches the JavaFX login screen and manages the primary stage.
 */
public class CrmDesktopApp extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        primaryStage.setTitle("CRM Desktop Application");

        showLoginScreen();

        primaryStage.setMinWidth(1024);
        primaryStage.setMinHeight(768);
        primaryStage.show();
    }

    /**
     * Navigates to the login screen.
     */
    public static void showLoginScreen() {
        try {
            SessionManager.getInstance().clearSession();
            FXMLLoader loader = new FXMLLoader(
                    CrmDesktopApp.class.getResource("/fxml/login.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 450, 500);
            scene.getStylesheets().add(
                    CrmDesktopApp.class.getResource("/css/styles.css").toExternalForm());
            primaryStage.setScene(scene);
            primaryStage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Navigates to the main application window after login.
     */
    public static void showMainWindow() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    CrmDesktopApp.class.getResource("/fxml/main.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 1280, 800);
            scene.getStylesheets().add(
                    CrmDesktopApp.class.getResource("/css/styles.css").toExternalForm());
            primaryStage.setScene(scene);
            primaryStage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
