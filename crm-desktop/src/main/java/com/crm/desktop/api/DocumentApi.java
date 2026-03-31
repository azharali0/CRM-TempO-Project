package com.crm.desktop.api;

import com.google.gson.JsonObject;

import java.net.http.HttpResponse;

/**
 * Document download API calls.
 */
public class DocumentApi {

    private final ApiClient client = ApiClient.getInstance();

    public byte[] downloadDocument(String documentId) throws Exception {
        HttpResponse<byte[]> response = client.getBytes(
                "/api/documents/" + documentId + "/download");
        if (response.statusCode() == 200) {
            return response.body();
        }
        return null;
    }

    public JsonObject deleteDocument(String documentId) throws Exception {
        HttpResponse<String> response = client.delete("/api/documents/" + documentId);
        return client.getGson().fromJson(response.body(), JsonObject.class);
    }
}
