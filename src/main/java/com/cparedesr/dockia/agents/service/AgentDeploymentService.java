/*
 * Copyright (c) 2026 cparedes. Todos los derechos reservados.
 */
package com.cparedesr.dockia.agents.service;

import com.cparedesr.dockia.agents.model.AgentDeployRequest;
import com.cparedesr.dockia.agents.model.AgentDeployResponse;
import com.cparedesr.dockia.agents.service.docker.DockerService;
import com.cparedesr.dockia.agents.service.registry.AgentRegistryService;
import com.cparedesr.dockia.agents.service.secrets.SecretsService;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * Orquesta el despliegue de un agente: resuelve secretos, prepara variables
 * de entorno seguras, arranca el contenedor y registra el resultado en el
 * repositorio.
 */
public class AgentDeploymentService {

    private SecretsService secretsService;
    private DockerService dockerService;
    private AgentRegistryService registryService;
    private Properties globalProperties;

    public void setSecretsService(SecretsService secretsService) { this.secretsService = secretsService; }
    public void setDockerService(DockerService dockerService) { this.dockerService = dockerService; }
    public void setRegistryService(AgentRegistryService registryService) { this.registryService = registryService; }
    public void setGlobalProperties(Properties globalProperties) { this.globalProperties = globalProperties; }

    public AgentDeployResponse deploy(AgentDeployRequest req) {

        String agentId = "agent-" + UUID.randomUUID();

        String authType = (req.getAlfresco().getAuthType() == null) ? "basic" : req.getAlfresco().getAuthType().toLowerCase();
        String alfrescoBaseUrl = normalizeBaseUrl(req.getAlfresco().getBaseUrl());

        String alfPass = null;
        if ("basic".equals(authType)) {
            alfPass = secretsService.resolve(req.getAlfresco().getPasswordSecretRef().getSecretRef());
        }

        String llmKey = (req.getLlm().getApiKeySecretRef() != null && StringUtils.hasText(req.getLlm().getApiKeySecretRef().getSecretRef()))
                ? secretsService.resolve(req.getLlm().getApiKeySecretRef().getSecretRef())
                : null;

        // Variables estandar que el runtime del agente espera recibir.
        Map<String,String> env = new HashMap<>();
        env.put("AGENT_ID", agentId);
        env.put("AGENT_NAME", req.getName());
        env.put("ALFRESCO_BASE_URL", alfrescoBaseUrl);
        env.put("ALFRESCO_AUTH_TYPE", authType);
        if (StringUtils.hasText(req.getAlfresco().getUsername())) env.put("ALFRESCO_USERNAME", req.getAlfresco().getUsername());
        if (alfPass != null) env.put("ALFRESCO_PASSWORD", alfPass);

        env.put("ALFRESCO_DOCUMENT_TYPE", req.getAlfresco().getDocumentType());
        env.put("ALFRESCO_EVENTS_BROKER_URL", req.getAlfresco().getEventsBrokerUrl());
        env.put("SPRING_ACTIVEMQ_BROKERURL", req.getAlfresco().getEventsBrokerUrl());
        env.put("SPRING_ACTIVEMQ_BROKER_URL", req.getAlfresco().getEventsBrokerUrl());

        env.put("LLM_PROVIDER", req.getLlm().getProvider());
        if (StringUtils.hasText(req.getLlm().getBaseUrl())) env.put("LLM_BASE_URL", req.getLlm().getBaseUrl());
        env.put("LLM_MODEL", req.getLlm().getModel());
        if (llmKey != null) env.put("LLM_API_KEY", llmKey);

        env.put("AGENT_PROMPT", req.getLlm().getPrompt());

        if (req.getEnv() != null) env.putAll(req.getEnv());

        // Las etiquetas permiten localizar el contenedor desde Docker sin
        // consultar primero Alfresco.
        Map<String,String> labels = Map.of(
                "com.cparedesr.dockia.agentId", agentId,
                "com.cparedesr.dockia.name", req.getName(),
                "com.cparedesr.dockia.documentType", req.getAlfresco().getDocumentType()
        );

        DockerService.CreateResult created = dockerService.createAndStart(agentId, req.getImage(), env, labels, null);

        AgentDeployRequest sanitized = sanitize(req, alfrescoBaseUrl);
        registryService.createAgentNode(agentId, sanitized, created.getContainerId(), "running", created.getCurrentState());

        return new AgentDeployResponse(
                agentId,
                req.getName(),
                "running",
                created.getCurrentState(),
                "/alfresco/api/-default-/public/ai-agents/versions/1/agents/" + agentId + "/status"
        );
    }

    private AgentDeployRequest sanitize(AgentDeployRequest in, String alfrescoBaseUrl) {
        AgentDeployRequest out = new AgentDeployRequest();
        out.setName(in.getName());
        out.setImage(in.getImage());
        out.setEnv(in.getEnv());

        AgentDeployRequest.AlfrescoConfig a = new AgentDeployRequest.AlfrescoConfig();
        a.setBaseUrl(alfrescoBaseUrl);
        a.setAuthType(in.getAlfresco().getAuthType());
        a.setUsername(in.getAlfresco().getUsername());
        // Se conserva la referencia al secreto, nunca el valor resuelto.
        a.setPasswordSecretRef(in.getAlfresco().getPasswordSecretRef());
        a.setDocumentType(in.getAlfresco().getDocumentType());
        a.setEventsBrokerUrl(in.getAlfresco().getEventsBrokerUrl());
        out.setAlfresco(a);

        AgentDeployRequest.LlmConfig l = new AgentDeployRequest.LlmConfig();
        l.setProvider(in.getLlm().getProvider());
        l.setBaseUrl(in.getLlm().getBaseUrl());
        l.setModel(in.getLlm().getModel());
        l.setApiKeySecretRef(in.getLlm().getApiKeySecretRef());
        l.setPrompt(in.getLlm().getPrompt());
        out.setLlm(l);

        return out;
    }

    private String normalizeBaseUrl(String baseUrl) {
        return baseUrl == null ? null : baseUrl.replaceAll("/+$", "");
    }
}
