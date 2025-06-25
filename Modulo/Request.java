package Modulo;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.Map;

public class Request {
    private String uri;
    private String apiKey;
    private String jsonBody;
    private String method = "GET";
    private final Map<String, String> queries = new LinkedHashMap<>();

    public Request uri(String uri) {
        this.uri = uri;
        return this;
    }

    public Request auth(String apiKey) {
        this.apiKey = apiKey;
        return this;
    }

    public Request json(String body) {
        this.jsonBody = body;
        return this;
    }

    public Request post() {
        this.method = "POST";
        return this;
    }

    public Request get() {
        this.method = "GET";
        return this;
    }

    public Request patch() {
        this.method = "PATCH";
        return this;
    }


    public Request query(String key, String value) {
        queries.put(key, value);
        return this;
    }

    public Request uriOut() {
    String fullUri = uri;

    if (!queries.isEmpty()) {
        StringBuilder sb = new StringBuilder(uri);
        sb.append(uri.contains("?") ? "&" : "?");
        for (Map.Entry<String, String> entry : queries.entrySet()) {
            sb.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
            sb.append("=");
            sb.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
            sb.append("&");
        }
        sb.setLength(sb.length() - 1); // remove last &
        fullUri = sb.toString();
    }

    System.out.println("🔗 Full URI: " + fullUri);
    return this;
}


    public Response send() {
        try {
            String fullUri = uri;

            if (!queries.isEmpty()) {
                StringBuilder sb = new StringBuilder(uri);
                sb.append(uri.contains("?") ? "&" : "?");
                for (Map.Entry<String, String> entry : queries.entrySet()) {
                    sb.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
                    sb.append("=");
                    sb.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
                    sb.append("&");
                }
                sb.setLength(sb.length() - 1); // remove last &
                fullUri = sb.toString();
            }

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(fullUri))
                    .header("apikey", apiKey)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json");

            if (method.equals("POST")) {
                builder.header("Prefer", "return=representation");
                builder.POST(HttpRequest.BodyPublishers.ofString(jsonBody));
            } else {
                builder.GET();
            }

            HttpRequest request = builder.build();
            HttpResponse<String> httpRes = client.send(request, HttpResponse.BodyHandlers.ofString());

            return new Response(httpRes.statusCode(), httpRes.body());

        } catch (Exception e) {
            return new Response(500, "Error: " + e.getMessage());
        }
    }
}
