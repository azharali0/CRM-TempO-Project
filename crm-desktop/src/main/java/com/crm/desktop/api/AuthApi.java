package com.crm.desktop.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.net.http.HttpResponse;

/**
 * Handles login, register, refresh, and logout API calls.
 */
public class AuthApi {

    private final ApiClient client = ApiClient.getInstance();

    /**
     * Sends login request. Returns parsed response.
     */
    public JsonObject login(String email, String password) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("email", email);
        body.addProperty("password", password);

        HttpResponse<String> response = client.postNoAuth("/api/auth/login", body);
        return client.getGson().fromJson(response.body(), JsonObject.class);
    }

    /**
     * Sends registration request.
     */
    public JsonObject register(String name, String email, String password, String role)
            throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("name", name);
        body.addProperty("email", email);
        body.addProperty("password", password);
        if (role != null) {
            body.addProperty("role", role);
        }

        HttpResponse<String> response = client.postNoAuth("/api/auth/register", body);
        return client.getGson().fromJson(response.body(), JsonObject.class);
    }

    /**
     * Refreshes the access token using a refresh token.
     */
    public JsonObject refreshToken(String refreshToken) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("token", refreshToken);

        HttpResponse<String> response = client.postNoAuth("/api/auth/refresh", body);
        return client.getGson().fromJson(response.body(), JsonObject.class);
    }

    /**
     * Revokes the current refresh token (logout).
     */
    public JsonObject logout(String refreshToken) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("token", refreshToken);

        HttpResponse<String> response = client.post("/api/auth/logout", body);
        return client.getGson().fromJson(response.body(), JsonObject.class);
    }
}
