/*
 * Copyright (c) 2026 cparedes. Todos los derechos reservados.
 */
package com.cparedesr.dockia.agents.extractmetadata;

import org.alfresco.event.sdk.handling.filter.EventFilter;
import org.alfresco.event.sdk.handling.filter.IsFileFilter;
import org.alfresco.event.sdk.handling.filter.NodeTypeFilter;
import org.alfresco.event.sdk.handling.handler.OnNodeCreatedEventHandler;
import org.alfresco.repo.event.v1.model.DataAttributes;
import org.alfresco.repo.event.v1.model.NodeResource;
import org.alfresco.repo.event.v1.model.RepoEvent;
import org.alfresco.repo.event.v1.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Component
public class ExtractMetadataEventHandler implements OnNodeCreatedEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExtractMetadataEventHandler.class);

    private final AgentProperties properties;
    private final AlfrescoContentClient contentClient;
    private final DocumentTextExtractor textExtractor;
    private final OllamaMetadataClient ollamaMetadataClient;
    private final AlfrescoMetadataWriter metadataWriter;

    public ExtractMetadataEventHandler(AgentProperties properties,
                                       AlfrescoContentClient contentClient,
                                       DocumentTextExtractor textExtractor,
                                       OllamaMetadataClient ollamaMetadataClient,
                                       AlfrescoMetadataWriter metadataWriter) {
        this.properties = properties;
        this.contentClient = contentClient;
        this.textExtractor = textExtractor;
        this.ollamaMetadataClient = ollamaMetadataClient;
        this.metadataWriter = metadataWriter;
    }

    @Override
    public void handleEvent(RepoEvent<DataAttributes<Resource>> event) {
        Resource resource = event.getData().getResource();
        if (!(resource instanceof NodeResource)) {
            LOGGER.debug("Evento ignorado porque el recurso no es un nodo: {}", resource);
            return;
        }

        NodeResource nodeResource = (NodeResource) resource;
        String nodeId = nodeResource.getId();
        String nodeName = nodeResource.getName();

        try {
            try (InputStream stream = contentClient.openContent(nodeId)) {
                String text = textExtractor.extract(stream);
                if (text.isBlank()) {
                    LOGGER.info("El nodo {} no contiene texto extraible", nodeId);
                    return;
                }

                MetadataExtractionResult result = ollamaMetadataClient.extract(nodeName, text);
                if (!result.hasValues()) {
                    LOGGER.info("No se extrajeron metadatos personales del nodo {}", nodeId);
                    return;
                }

                metadataWriter.write(nodeId, result);
                LOGGER.info("Metadatos personales extraidos y guardados en el nodo {}", nodeId);
            }
        } catch (Exception e) {
            LOGGER.error("Error procesando nodo {} con el agente extract-metadata", nodeId, e);
        }
    }

    @Override
    public EventFilter getEventFilter() {
        return IsFileFilter.get().and(NodeTypeFilter.of(properties.getDocumentType()));
    }
}
