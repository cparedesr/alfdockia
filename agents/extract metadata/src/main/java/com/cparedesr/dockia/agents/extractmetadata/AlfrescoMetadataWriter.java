/*
 * Copyright (c) 2026 cparedes. Todos los derechos reservados.
 */
package com.cparedesr.dockia.agents.extractmetadata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class AlfrescoMetadataWriter {

    private final AgentProperties properties;
    private final ObjectMapper objectMapper;
    private final AlfrescoClientSupport clientSupport;
    private final HttpClient httpClient;

    public AlfrescoMetadataWriter(AgentProperties properties,
                                  ObjectMapper objectMapper,
                                  AlfrescoClientSupport clientSupport) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.clientSupport = clientSupport;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public void write(String nodeId, MetadataExtractionResult result) throws IOException, InterruptedException {
        if (!result.hasValues()) {
            return;
        }

        List<String> aspects = loadAspectNames(nodeId);
        if (!aspects.contains(properties.getMetadataAspect())) {
            aspects.add(properties.getMetadataAspect());
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("aspectNames", aspects);
        body.put("properties", result.toAlfrescoProperties());

        HttpRequest request = clientSupport.baseRequest(clientSupport.nodeUrl(nodeId))
                .PUT(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .header("Content-Type", "application/json")
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Alfresco returned HTTP " + response.statusCode() + ": " + safeBody(response.body()));
        }
    }

    private List<String> loadAspectNames(String nodeId) throws IOException, InterruptedException {
        HttpRequest request = clientSupport.baseRequest(clientSupport.nodeUrl(nodeId) + "?include=aspectNames")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Alfresco returned HTTP " + response.statusCode() + ": " + safeBody(response.body()));
        }

        JsonNode aspectNames = objectMapper.readTree(response.body())
                .path("entry")
                .path("aspectNames");

        List<String> aspects = new ArrayList<>();
        if (aspectNames.isArray()) {
            aspectNames.forEach(aspect -> aspects.add(aspect.asText()));
        }
        return aspects;
    }

    private String safeBody(String body) {
        if (body == null) {
            return "";
        }
        String trimmed = body.trim();
        return trimmed.length() > 500 ? trimmed.substring(0, 500) + "..." : trimmed;
    }
}
