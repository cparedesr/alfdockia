/*
 * Copyright (c) 2026 cparedes. Todos los derechos reservados.
 */
package com.cparedesr.dockia.agents.service;

import com.cparedesr.dockia.agents.model.AgentDeployRequest;
import com.cparedesr.dockia.agents.service.exception.BadRequestException;
import com.cparedesr.dockia.agents.service.registry.AgentRegistryService;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.*;

public class AgentValidationServiceTest {

    private AgentValidationService validationService;
    private AgentRegistryService registryService;

    @Before
    public void setUp() {
        validationService = new AgentValidationService();
        registryService = mock(AgentRegistryService.class);

        validationService.setRegistryService(registryService);
    }

    @Test
    public void validRequestPasses() {
        AgentDeployRequest req = validRequest();

        when(registryService.existsByName("agent1")).thenReturn(false);

        validationService.validateDeployRequest(req);
    }

    @Test(expected = BadRequestException.class)
    public void duplicateNameFails() {
        AgentDeployRequest req = validRequest();
        when(registryService.existsByName("agent1")).thenReturn(true);

        validationService.validateDeployRequest(req);
    }

    @Test(expected = BadRequestException.class)
    public void missingDocumentTypeFails() {
        AgentDeployRequest req = validRequest();
        req.getAlfresco().setDocumentType(null);

        when(registryService.existsByName("agent1")).thenReturn(false);

        validationService.validateDeployRequest(req);
    }

    @Test(expected = BadRequestException.class)
    public void invalidDocumentTypeFails() {
        AgentDeployRequest req = validRequest();
        req.getAlfresco().setDocumentType("content");

        when(registryService.existsByName("agent1")).thenReturn(false);

        validationService.validateDeployRequest(req);
    }

    @Test(expected = BadRequestException.class)
    public void basicAuthRequiresUsername() {
        AgentDeployRequest req = validRequest();
        req.getAlfresco().setUsername(null);

        when(registryService.existsByName("agent1")).thenReturn(false);

        validationService.validateDeployRequest(req);
    }

    @Test(expected = BadRequestException.class)
    public void portsAreNotAllowed() {
        AgentDeployRequest req = validRequest();
        AgentDeployRequest.PortMapping p = new AgentDeployRequest.PortMapping();
        p.setContainerPort(8080);
        p.setHostPort(9090);
        req.setPorts(java.util.List.of(p));

        when(registryService.existsByName("agent1")).thenReturn(false);

        validationService.validateDeployRequest(req);
    }

    private AgentDeployRequest validRequest() {
        AgentDeployRequest req = new AgentDeployRequest();
        req.setName("agent1");
        req.setImage("registry.tuorg.com/agents/summarizer:1.0.0");

        AgentDeployRequest.AlfrescoConfig a = new AgentDeployRequest.AlfrescoConfig();
        a.setBaseUrl("http://localhost:8080/alfresco");
        a.setAuthType("basic");
        a.setUsername("svc_ai");
        AgentDeployRequest.SecretRef sr = new AgentDeployRequest.SecretRef();
        sr.setSecretRef("prop:alfresco.aiagents.secret.svc_ai_password");
        a.setPasswordSecretRef(sr);
        a.setDocumentType("cm:content");
        a.setEventsBrokerUrl("tcp://alfresco-dockia-agents-activemq:61616");
        req.setAlfresco(a);

        AgentDeployRequest.LlmConfig llm = new AgentDeployRequest.LlmConfig();
        llm.setProvider("ollama");
        llm.setBaseUrl("http://ollama:11434");
        llm.setModel("llama3.1");
        llm.setPrompt("hola");
        req.setLlm(llm);

        return req;
    }
}
