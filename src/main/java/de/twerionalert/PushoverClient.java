package de.twerionalert;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;

final class PushoverClient {
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    void send(AlertConfig config, String title, String message) throws IOException, InterruptedException {
        var fields = new LinkedHashMap<String, String>();
        fields.put("token", config.applicationToken);
        fields.put("user", config.userKey);
        fields.put("title", title);
        fields.put("message", message.substring(0, Math.min(message.length(), 1000)));
        if (!config.device.isBlank()) fields.put("device", config.device);
        String body = fields.entrySet().stream()
            .map(e -> encode(e.getKey()) + "=" + encode(e.getValue()))
            .collect(java.util.stream.Collectors.joining("&"));
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.pushover.net/1/messages.json"))
            .timeout(Duration.ofSeconds(15))
            .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        var response = http.send(request, HttpResponse.BodyHandlers.ofString());
        // Do not log responses or request data: they may contain sensitive details.
        if (response.statusCode() != 200) throw new IOException("Pushover HTTP " + response.statusCode());
        try {
            if (com.google.gson.JsonParser.parseString(response.body()).getAsJsonObject().get("status").getAsInt() != 1)
                throw new IOException("Pushover rejected notification");
        } catch (RuntimeException e) {
            throw new IOException("Invalid Pushover response");
        }
    }

    private static String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
}
