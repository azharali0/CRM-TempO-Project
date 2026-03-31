package com.crm.desktop.api;

import com.google.gson.JsonObject;

import java.net.http.HttpResponse;

/**
 * Interaction logging and timeline API calls.
 */
public class InteractionApi {

    private final ApiClient client = ApiClient.getInstance();

    public JsonObject getCustomerInteractions(String customerId, int page, int size)
            throws Exception {
        HttpResponse<String> response = client.get(
                "/api/interactions/customer/" + customerId + "?page=" + page + "&size=" + size);
        return client.getGson().fromJson(response.body(), JsonObject.class);
    }

    public JsonObject getRecentInteractions() throws Exception {
        HttpResponse<String> response = client.get("/api/interactions/recent");
        return client.getGson().fromJson(response.body(), JsonObject.class);
    }

    public JsonObject createInteraction(JsonObject interactionData) throws Exception {
        HttpResponse<String> response = client.post("/api/interactions", interactionData);
        return client.getGson().fromJson(response.body(), JsonObject.class);
    }

    public JsonObject updateInteraction(String id, JsonObject updateData) throws Exception {
        HttpResponse<String> response = client.put("/api/interactions/" + id, updateData);
        return client.getGson().fromJson(response.body(), JsonObject.class);
    }
}
