package com.crm.desktop.api;

import com.google.gson.JsonObject;

import java.net.http.HttpResponse;

/**
 * Lead CRUD and stage transition API calls.
 */
public class LeadApi {

    private final ApiClient client = ApiClient.getInstance();

    public JsonObject getLeads(int page, int size, String stageFilter) throws Exception {
        StringBuilder path = new StringBuilder("/api/leads?page=" + page + "&size=" + size);
        if (stageFilter != null && !stageFilter.isBlank()) {
            path.append("&stage=").append(stageFilter);
        }
        HttpResponse<String> response = client.get(path.toString());
        return client.getGson().fromJson(response.body(), JsonObject.class);
    }

    public JsonObject getLeadById(String id) throws Exception {
        HttpResponse<String> response = client.get("/api/leads/" + id);
        return client.getGson().fromJson(response.body(), JsonObject.class);
    }

    public JsonObject createLead(JsonObject leadData) throws Exception {
        HttpResponse<String> response = client.post("/api/leads", leadData);
        return client.getGson().fromJson(response.body(), JsonObject.class);
    }

    public JsonObject updateLead(String id, JsonObject leadData) throws Exception {
        HttpResponse<String> response = client.put("/api/leads/" + id, leadData);
        return client.getGson().fromJson(response.body(), JsonObject.class);
    }

    public JsonObject changeStage(String id, String newStage, String lostReason)
            throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("newStage", newStage);
        if (lostReason != null) {
            body.addProperty("lostReason", lostReason);
        }
        HttpResponse<String> response = client.patch("/api/leads/" + id + "/stage", body);
        return client.getGson().fromJson(response.body(), JsonObject.class);
    }
}
