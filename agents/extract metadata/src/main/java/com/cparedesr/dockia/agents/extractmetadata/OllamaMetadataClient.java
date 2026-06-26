/*
 * Copyright (c) 2026 cparedes. Todos los derechos reservados.
 */
package com.cparedesr.dockia.agents.extractmetadata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class OllamaMetadataClient {

    private final AgentProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public OllamaMetadataClient(AgentProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public MetadataExtractionResult extract(String nodeName, String text) throws IOException, InterruptedException {
        if (!"ollama".equalsIgnoreCase(properties.getLlm().getProvider())) {
            throw new IllegalStateException("This agent currently supports LLM_PROVIDER=ollama");
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", properties.getLlm().getModel());
        body.put("prompt", buildPrompt(nodeName, text));
        body.put("stream", false);
        body.put("format", "json");

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(properties.getLlm().getBaseUrl() + "/api/generate"))
                .timeout(Duration.ofMinutes(2))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)));

        if (properties.getLlm().getApiKey() != null && !properties.getLlm().getApiKey().isBlank()) {
            requestBuilder.header("Authorization", "Bearer " + properties.getLlm().getApiKey());
        }

        HttpResponse<String> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Ollama returned HTTP " + response.statusCode() + ": " + safeBody(response.body()));
        }

        JsonNode root = objectMapper.readTree(response.body());
        String json = stripJson(root.path("response").asText(""));
        return objectMapper.readValue(json, MetadataExtractionResult.class);
    }

    private String buildPrompt(String nodeName, String text) {
        return properties.getLlm().getPrompt()
                + "\n\nDocumento: " + nullSafe(nodeName)
                + "\n\nExtrae estos campos del texto: nombre, apellido1, apellido2, dni, fechaNacimiento."
                + "\nfechaNacimiento debe estar en formato ISO yyyy-MM-dd."
                + "\nSi no encuentras un campo, usa null."
                + "\nDevuelve exclusivamente este JSON sin markdown:"
                + "\n{\"nombre\":null,\"apellido1\":null,\"apellido2\":null,\"dni\":null,\"fechaNacimiento\":null}"
                + "\n\nTexto:\n" + text;
    }

    private String stripJson(String value) {
        String cleaned = value == null ? "" : value.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceFirst("^```[a-zA-Z]*", "").replaceFirst("```$", "").trim();
        }
        int first = cleaned.indexOf('{');
        int last = cleaned.lastIndexOf('}');
        if (first >= 0 && last > first) {
            return cleaned.substring(first, last + 1);
        }
        return cleaned;
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private String safeBody(String body) {
        if (body == null) {
            return "";
        }
        String trimmed = body.trim();
        return trimmed.length() > 500 ? trimmed.substring(0, 500) + "..." : trimmed;
    }
}
