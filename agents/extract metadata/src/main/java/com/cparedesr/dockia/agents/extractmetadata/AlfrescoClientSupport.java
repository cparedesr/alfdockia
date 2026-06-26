/*
 * Copyright (c) 2026 cparedes. Todos los derechos reservados.
 */
package com.cparedesr.dockia.agents.extractmetadata;

import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

@Component
public class AlfrescoClientSupport {

    private final AgentProperties properties;

    public AlfrescoClientSupport(AgentProperties properties) {
        this.properties = properties;
    }

    public HttpRequest.Builder baseRequest(String url) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("Accept", "application/json");

        String authType = properties.getAlfrescoAuthType() == null ? "basic" : properties.getAlfrescoAuthType().toLowerCase();
        if ("basic".equals(authType)) {
            String credentials = properties.getAlfrescoUsername() + ":" + properties.getAlfrescoPassword();
            builder.header("Authorization", "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8)));
        } else if ("bearer".equals(authType) && properties.getAlfrescoPassword() != null && !properties.getAlfrescoPassword().isBlank()) {
            builder.header("Authorization", "Bearer " + properties.getAlfrescoPassword());
        }

        return builder;
    }

    public String nodeUrl(String nodeId) {
        return properties.getAlfrescoBaseUrl() + "/api/-default-/public/alfresco/versions/1/nodes/" + nodeId;
    }
}
