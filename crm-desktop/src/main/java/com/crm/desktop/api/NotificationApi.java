package com.crm.desktop.api;

import com.google.gson.JsonObject;

import java.net.http.HttpResponse;

/**
 * Notification polling, unread count, and mark-as-read API calls.
 */
public class NotificationApi {

    private final ApiClient client = ApiClient.getInstance();

    public JsonObject getMyNotifications(int page, int size) throws Exception {
        HttpResponse<String> response = client.get(
                "/api/notifications/my?page=" + page + "&size=" + size);
        return client.getGson().fromJson(response.body(), JsonObject.class);
    }

    public JsonObject getUnreadCount() throws Exception {
        HttpResponse<String> response = client.get("/api/notifications/unread-count");
        return client.getGson().fromJson(response.body(), JsonObject.class);
    }

    public JsonObject markAsRead(String id) throws Exception {
        HttpResponse<String> response = client.patch(
                "/api/notifications/" + id + "/read", null);
        return client.getGson().fromJson(response.body(), JsonObject.class);
    }
}
