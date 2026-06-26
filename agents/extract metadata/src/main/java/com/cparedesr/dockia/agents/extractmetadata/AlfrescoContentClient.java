/*
 * Copyright (c) 2026 cparedes. Todos los derechos reservados.
 */
package com.cparedesr.dockia.agents.extractmetadata;

import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class AlfrescoContentClient {

    private final AlfrescoClientSupport clientSupport;
    private final HttpClient httpClient;

    public AlfrescoContentClient(AlfrescoClientSupport clientSupport) {
        this.clientSupport = clientSupport;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public InputStream openContent(String nodeId) throws Exception {
        HttpRequest request = clientSupport.baseRequest(clientSupport.nodeUrl(nodeId) + "/content?attachment=false")
                .header("Accept", "*/*")
                .GET()
                .build();

        for (int attempt = 1; attempt <= 3; attempt++) {
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return response.body();
            }

            response.body().close();
            if (attempt == 3 || !isRetryable(response.statusCode())) {
                throw new IllegalStateException("Alfresco content API returned HTTP " + response.statusCode());
            }
            Thread.sleep(1000L * attempt);
        }
        throw new IllegalStateException("Alfresco content API did not return content");
    }

    private boolean isRetryable(int statusCode) {
        return statusCode == 404 || statusCode == 409 || statusCode >= 500;
    }
}
