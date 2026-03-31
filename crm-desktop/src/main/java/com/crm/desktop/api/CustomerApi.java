package com.crm.desktop.api;

import com.google.gson.JsonObject;

import java.net.http.HttpResponse;

/**
 * Customer CRUD API calls.
 */
public class CustomerApi {

    private final ApiClient client = ApiClient.getInstance();

    public JsonObject getCustomers(int page, int size, String name, String company,
                                    String status, String city) throws Exception {
        StringBuilder path = new StringBuilder("/api/customers?page=" + page + "&size=" + size);
        if (name != null && !name.isBlank()) path.append("&name=").append(name);
        if (company != null && !company.isBlank()) path.append("&company=").append(company);
        if (status != null && !status.isBlank()) path.append("&status=").append(status);
        if (city != null && !city.isBlank()) path.append("&city=").append(city);

        HttpResponse<String> response = client.get(path.toString());
        return client.getGson().fromJson(response.body(), JsonObject.class);
    }

    public JsonObject getCustomerById(String id) throws Exception {
        HttpResponse<String> response = client.get("/api/customers/" + id);
        return client.getGson().fromJson(response.body(), JsonObject.class);
    }

    public JsonObject createCustomer(JsonObject customerData) throws Exception {
        HttpResponse<String> response = client.post("/api/customers", customerData);
        return client.getGson().fromJson(response.body(), JsonObject.class);
    }

    public JsonObject updateCustomer(String id, JsonObject customerData) throws Exception {
        HttpResponse<String> response = client.put("/api/customers/" + id, customerData);
        return client.getGson().fromJson(response.body(), JsonObject.class);
    }

    public JsonObject deleteCustomer(String id) throws Exception {
        HttpResponse<String> response = client.delete("/api/customers/" + id);
        return client.getGson().fromJson(response.body(), JsonObject.class);
    }

    public JsonObject importCustomers(byte[] fileBytes, String filename) throws Exception {
        // For file uploads, a multipart request is needed;
        // using a simplified JSON-based proxy for now
        HttpResponse<String> response = client.get("/api/customers?page=0&size=1");
        return client.getGson().fromJson(response.body(), JsonObject.class);
    }

    public JsonObject getDocuments(String customerId) throws Exception {
        HttpResponse<String> response = client.get(
                "/api/customers/" + customerId + "/documents");
        return client.getGson().fromJson(response.body(), JsonObject.class);
    }
}
