/*
 * Copyright (c) 2026 cparedes. Todos los derechos reservados.
 */
package com.cparedesr.dockia.agents.service.subsystem;

import com.cparedesr.dockia.agents.service.AgentDeleteService;
import com.cparedesr.dockia.agents.service.AgentDeploymentService;
import com.cparedesr.dockia.agents.service.AgentValidationService;
import com.cparedesr.dockia.agents.service.exception.BadRequestException;
import com.cparedesr.dockia.agents.service.registry.AgentRegistryService;
import org.alfresco.repo.management.subsystems.ApplicationContextFactory;
import org.springframework.context.ApplicationContext;

/**
 * Punto de acceso desde el contexto padre hacia los servicios que viven dentro
 * del subsistema DockIA Agents.
 */
public class AgentSubsystemServiceLocator {

    private ApplicationContextFactory applicationContextFactory;

    public void setApplicationContextFactory(ApplicationContextFactory applicationContextFactory) {
        this.applicationContextFactory = applicationContextFactory;
    }

    public AgentValidationService getValidationService() {
        return getBean("dockia.agents.validationService", AgentValidationService.class);
    }

    public AgentDeploymentService getDeploymentService() {
        return getBean("dockia.agents.deploymentService", AgentDeploymentService.class);
    }

    public AgentRegistryService getRegistryService() {
        return getBean("dockia.agents.registryService", AgentRegistryService.class);
    }

    public AgentDeleteService getDeleteService() {
        return getBean("dockia.agents.deleteService", AgentDeleteService.class);
    }

    private <T> T getBean(String beanName, Class<T> beanType) {
        if (applicationContextFactory == null) {
            throw new BadRequestException("SUBSYSTEM_NOT_CONFIGURED", "DockIA Agents subsystem is not configured");
        }

        ApplicationContext context = applicationContextFactory.getApplicationContext();
        if (context == null) {
            throw new BadRequestException("SUBSYSTEM_NOT_AVAILABLE", "DockIA Agents subsystem is not available");
        }

        return context.getBean(beanName, beanType);
    }
}
