package com.crm.desktop.api;

import com.crm.desktop.service.SessionManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * HTTP client wrapper that adds JWT Authorization header to every request.
 * Handles token refresh automatically when a 401 is received.
 */
public class ApiClient {

    private static final String BASE_URL_DEFAULT = "http://localhost:8080";
    private static ApiClient instance;

    private final HttpClient httpClient;
    private final Gson gson;
    private String baseUrl;

    private ApiClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.gson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss")
                .create();
        this.baseUrl = System.getProperty("crm.api.url", BASE_URL_DEFAULT);
    }

    public static synchronized ApiClient getInstance() {
        if (instance == null) {
            instance = new ApiClient();
        }
        return instance;
    }

    public Gson getGson() {
        return gson;
    }

    /**
     * Sends a GET request with JWT authorization.
     */
    public HttpResponse<String> get(String path) throws IOException, InterruptedException {
        HttpRequest request = buildRequest(path)
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 401) {
            if (tryRefreshToken()) {
                request = buildRequest(path).GET().build();
                return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            }
        }
        return response;
    }

    /**
     * Sends a POST request with JSON body and JWT authorization.
     */
    public HttpResponse<String> post(String path, Object body)
            throws IOException, InterruptedException {
        String json = gson.toJson(body);
        HttpRequest request = buildRequest(path)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .header("Content-Type", "application/json")
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 401 && !path.contains("/auth/")) {
            if (tryRefreshToken()) {
                request = buildRequest(path)
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .header("Content-Type", "application/json")
                        .build();
                return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            }
        }
        return response;
    }

    /**
     * Sends a POST request without a body (for auth endpoints).
     */
    public HttpResponse<String> postNoAuth(String path, Object body)
            throws IOException, InterruptedException {
        String json = gson.toJson(body);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(15))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Sends a PUT request with JSON body and JWT authorization.
     */
    public HttpResponse<String> put(String path, Object body)
            throws IOException, InterruptedException {
        String json = gson.toJson(body);
        HttpRequest request = buildRequest(path)
                .PUT(HttpRequest.BodyPublishers.ofString(json))
                .header("Content-Type", "application/json")
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 401) {
            if (tryRefreshToken()) {
                request = buildRequest(path)
                        .PUT(HttpRequest.BodyPublishers.ofString(json))
                        .header("Content-Type", "application/json")
                        .build();
                return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            }
        }
        return response;
    }

    /**
     * Sends a PATCH request with JSON body and JWT authorization.
     */
    public HttpResponse<String> patch(String path, Object body)
            throws IOException, InterruptedException {
        String json = body != null ? gson.toJson(body) : "";
        HttpRequest request = buildRequest(path)
                .method("PATCH", HttpRequest.BodyPublishers.ofString(json))
                .header("Content-Type", "application/json")
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 401) {
            if (tryRefreshToken()) {
                request = buildRequest(path)
                        .method("PATCH", HttpRequest.BodyPublishers.ofString(json))
                        .header("Content-Type", "application/json")
                        .build();
                return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            }
        }
        return response;
    }

    /**
     * Sends a DELETE request with JWT authorization.
     */
    public HttpResponse<String> delete(String path) throws IOException, InterruptedException {
        HttpRequest request = buildRequest(path)
                .DELETE()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 401) {
            if (tryRefreshToken()) {
                request = buildRequest(path).DELETE().build();
                return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            }
        }
        return response;
    }

    /**
     * Returns a raw GET response for binary downloads (e.g., PDF, file download).
     */
    public HttpResponse<byte[]> getBytes(String path) throws IOException, InterruptedException {
        HttpRequest request = buildRequest(path).GET().build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
    }

    private HttpRequest.Builder buildRequest(String path) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(15));

        String token = SessionManager.getInstance().getAccessToken();
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        return builder;
    }

    /**
     * Attempts to refresh the access token using the stored refresh token.
     */
    private boolean tryRefreshToken() {
        try {
            String refreshToken = SessionManager.getInstance().getRefreshToken();
            if (refreshToken == null) return false;

            JsonObject body = new JsonObject();
            body.addProperty("token", refreshToken);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/auth/refresh"))
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonObject json = gson.fromJson(response.body(), JsonObject.class);
                JsonElement data = json.get("data");
                if (data != null && data.isJsonObject()) {
                    String newAccessToken = data.getAsJsonObject()
                            .get("accessToken").getAsString();
                    SessionManager.getInstance().updateAccessToken(newAccessToken);
                    return true;
                }
            }
        } catch (Exception e) {
            // Refresh failed
        }
        return false;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}
