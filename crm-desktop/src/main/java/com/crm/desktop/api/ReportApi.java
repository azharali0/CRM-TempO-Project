package com.crm.desktop.api;

import com.google.gson.JsonObject;

import java.net.http.HttpResponse;

/**
 * Dashboard, conversion, sales-by-rep, monthly trend,
 * activity summary, and PDF export API calls.
 */
public class ReportApi {

    private final ApiClient client = ApiClient.getInstance();

    public JsonObject getDashboard() throws Exception {
        HttpResponse<String> response = client.get("/api/reports/dashboard");
        return client.getGson().fromJson(response.body(), JsonObject.class);
    }

    public JsonObject getConversionReport() throws Exception {
        HttpResponse<String> response = client.get("/api/reports/conversion");
        return client.getGson().fromJson(response.body(), JsonObject.class);
    }

    public JsonObject getSalesByRep() throws Exception {
        HttpResponse<String> response = client.get("/api/reports/sales-by-rep");
        return client.getGson().fromJson(response.body(), JsonObject.class);
    }

    public JsonObject getMonthlyTrend() throws Exception {
        HttpResponse<String> response = client.get("/api/reports/monthly-trend");
        return client.getGson().fromJson(response.body(), JsonObject.class);
    }

    public JsonObject getActivitySummary() throws Exception {
        HttpResponse<String> response = client.get("/api/reports/activity-summary");
        return client.getGson().fromJson(response.body(), JsonObject.class);
    }

    public byte[] downloadPdf(String type, String id) throws Exception {
        String path = "/api/reports/export/pdf?type=" + type;
        if (id != null) {
            path += "&id=" + id;
        }
        HttpResponse<byte[]> response = client.getBytes(path);
        return response.body();
    }
}
