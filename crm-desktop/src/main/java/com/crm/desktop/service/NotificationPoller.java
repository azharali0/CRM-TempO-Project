package com.crm.desktop.service;

import com.crm.desktop.CrmDesktopApp;
import com.crm.desktop.api.NotificationApi;
import com.google.gson.JsonObject;
import javafx.application.Platform;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Background thread that polls GET /api/notifications/my every 30 seconds.
 * Uses JWT for authentication. If tokens expire, attempts auto-refresh.
 * If refresh fails, forces logout.
 */
public class NotificationPoller {

    private static final int POLL_INTERVAL_SECONDS = 30;

    private final NotificationApi notificationApi;
    private ScheduledExecutorService scheduler;
    private Consumer<Long> onUnreadCountChanged;

    public NotificationPoller() {
        this.notificationApi = new NotificationApi();
    }

    /**
     * Sets a callback invoked on the JavaFX thread when the unread count changes.
     */
    public void setOnUnreadCountChanged(Consumer<Long> callback) {
        this.onUnreadCountChanged = callback;
    }

    /**
     * Starts polling every 30 seconds.
     */
    public void start() {
        stop();
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "notification-poller");
            t.setDaemon(true);
            return t;
        });

        scheduler.scheduleAtFixedRate(this::poll, 0, POLL_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * Stops the polling thread.
     */
    public void stop() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
    }

    private void poll() {
        try {
            SessionManager session = SessionManager.getInstance();

            // Check session validity
            if (!session.isLoggedIn()) {
                stop();
                return;
            }

            // Check idle timeout — force logout if expired
            if (session.isSessionExpired()) {
                Platform.runLater(() -> {
                    stop();
                    CrmDesktopApp.showLoginScreen();
                });
                return;
            }

            // Poll unread count
            JsonObject response = notificationApi.getUnreadCount();
            if (response != null && response.has("data")) {
                JsonObject data = response.getAsJsonObject("data");
                if (data.has("unreadCount")) {
                    long count = data.get("unreadCount").getAsLong();
                    if (onUnreadCountChanged != null) {
                        Platform.runLater(() -> onUnreadCountChanged.accept(count));
                    }
                }
            }
        } catch (Exception e) {
            // Silently ignore polling failures to avoid crashing the app
        }
    }
}
