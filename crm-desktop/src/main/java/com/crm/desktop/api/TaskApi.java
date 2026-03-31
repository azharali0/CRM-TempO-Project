package com.crm.desktop.api;

import com.google.gson.JsonObject;

import java.net.http.HttpResponse;

/**
 * Task CRUD, my-tasks, and complete API calls.
 */
public class TaskApi {

    private final ApiClient client = ApiClient.getInstance();

    public JsonObject getTasks(int page, int size, String statusFilter) throws Exception {
        StringBuilder path = new StringBuilder("/api/tasks?page=" + page + "&size=" + size);
        if (statusFilter != null && !statusFilter.isBlank()) {
            path.append("&status=").append(statusFilter);
        }
        HttpResponse<String> response = client.get(path.toString());
        return client.getGson().fromJson(response.body(), JsonObject.class);
    }

    public JsonObject getMyTasks(int page, int size, String statusFilter) throws Exception {
        StringBuilder path = new StringBuilder("/api/tasks/my?page=" + page + "&size=" + size);
        if (statusFilter != null && !statusFilter.isBlank()) {
            path.append("&status=").append(statusFilter);
        }
        HttpResponse<String> response = client.get(path.toString());
        return client.getGson().fromJson(response.body(), JsonObject.class);
    }

    public JsonObject getTaskById(String id) throws Exception {
        HttpResponse<String> response = client.get("/api/tasks/" + id);
        return client.getGson().fromJson(response.body(), JsonObject.class);
    }

    public JsonObject createTask(JsonObject taskData) throws Exception {
        HttpResponse<String> response = client.post("/api/tasks", taskData);
        return client.getGson().fromJson(response.body(), JsonObject.class);
    }

    public JsonObject updateTask(String id, JsonObject taskData) throws Exception {
        HttpResponse<String> response = client.put("/api/tasks/" + id, taskData);
        return client.getGson().fromJson(response.body(), JsonObject.class);
    }

    public JsonObject completeTask(String id) throws Exception {
        HttpResponse<String> response = client.patch("/api/tasks/" + id + "/complete", null);
        return client.getGson().fromJson(response.body(), JsonObject.class);
    }
}
