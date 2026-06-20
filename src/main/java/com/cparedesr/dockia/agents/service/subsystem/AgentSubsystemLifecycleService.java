/*
 * Copyright (c) 2026 cparedes. Todos los derechos reservados.
 */
package com.cparedesr.dockia.agents.service.subsystem;

import com.cparedesr.dockia.agents.model.AgentRuntimeInfo;
import com.cparedesr.dockia.agents.service.docker.DockerService;
import com.cparedesr.dockia.agents.service.registry.AgentRegistryService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.SmartLifecycle;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Coordina el arranque y la parada de los runtimes cuando Alfresco inicia o
 * detiene el subsistema DockIA Agents.
 */
public class AgentSubsystemLifecycleService implements SmartLifecycle {

    private static final Log LOGGER = LogFactory.getLog(AgentSubsystemLifecycleService.class);
    private static final String STARTUP_BANNER = String.join(System.lineSeparator(),
            "",
            "==============================================================",
            "                         ALFDOCKIA",
            "            Integracion nativa de Alfresco con Docker",
            " Copyright (c) 2026 cparedes. Todos los derechos reservados.",
            "==============================================================");

    private AgentRegistryService registryService;
    private DockerService dockerService;
    private Properties globalProperties;
    private volatile boolean running;
    private final Set<String> knownContainerIds = new LinkedHashSet<>();

    public void setRegistryService(AgentRegistryService registryService) {
        this.registryService = registryService;
    }

    public void setDockerService(DockerService dockerService) {
        this.dockerService = dockerService;
    }

    public void setGlobalProperties(Properties globalProperties) {
        this.globalProperties = globalProperties;
    }

    @Override
    public synchronized void start() {
        if (running) {
            return;
        }

        writeToContainerLog(STARTUP_BANNER);
        LOGGER.info(STARTUP_BANNER);

        if (!isStartAgentsOnStart()) {
            running = true;
            LOGGER.info("Arranque automatico de agentes desactivado para el subsistema DockIA Agents");
            return;
        }

        List<AgentRuntimeInfo> runtimes = loadAllRuntimeInfos();
        String startupMessage = "[ALFDOCKIA] Subsistema inicializado. Contenedores administrados encontrados: "
                + runtimes.size();
        writeToContainerLog(startupMessage);
        LOGGER.info(startupMessage);
        for (AgentRuntimeInfo runtime : runtimes) {
            String containerId = normalize(runtime.getContainerId());
            if (containerId == null) {
                continue;
            }

            try {
                knownContainerIds.add(containerId);
                dockerService.start(containerId);
                LOGGER.info("Agente " + runtime.getAgentId() + " arrancado al iniciar el subsistema");
            } catch (RuntimeException e) {
                LOGGER.warn("No se pudo arrancar el agente " + runtime.getAgentId()
                        + " con contenedor " + containerId + " al iniciar el subsistema", e);
            }
        }

        running = true;
    }

    @Override
    public synchronized void stop() {
        if (!running) {
            return;
        }

        if (!isStopAgentsOnStop()) {
            running = false;
            LOGGER.info("Parada automatica de agentes desactivada para el subsistema DockIA Agents");
            return;
        }

        int timeout = getStopTimeoutSeconds();
        List<AgentRuntimeInfo> runtimes = loadAllRuntimeInfos();
        String shutdownMessage = "[ALFDOCKIA] Deteniendo el subsistema. Contenedores administrados encontrados: "
                + runtimes.size();
        writeToContainerLog(shutdownMessage);
        LOGGER.info(shutdownMessage);
        for (AgentRuntimeInfo runtime : runtimes) {
            String containerId = normalize(runtime.getContainerId());
            if (containerId == null) {
                continue;
            }

            try {
                dockerService.stop(containerId, timeout);
                LOGGER.info("Agente " + runtime.getAgentId() + " parado al detener el subsistema");
            } catch (RuntimeException e) {
                LOGGER.warn("No se pudo parar el agente " + runtime.getAgentId()
                        + " con contenedor " + containerId + " al detener el subsistema", e);
            }
        }

        running = false;
    }

    @Override
    public void stop(Runnable callback) {
        try {
            stop();
        } finally {
            callback.run();
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public int getPhase() {
        return 0;
    }

    private List<AgentRuntimeInfo> loadRuntimeInfos() {
        try {
            return registryService.listRuntimeInfos();
        } catch (RuntimeException e) {
            LOGGER.warn("No se pudo leer el registro de agentes del subsistema DockIA Agents", e);
            return List.of();
        }
    }

    private List<AgentRuntimeInfo> loadAllRuntimeInfos() {
        Map<String, AgentRuntimeInfo> runtimesByContainer = new LinkedHashMap<>();
        for (AgentRuntimeInfo runtime : loadRuntimeInfos()) {
            addRuntime(runtimesByContainer, runtime);
        }

        for (String containerId : knownContainerIds) {
            addRuntime(runtimesByContainer, runtimeFromContainerId(containerId));
        }

        try {
            for (String containerId : dockerService.listManagedContainerIds()) {
                addRuntime(runtimesByContainer, runtimeFromContainerId(containerId));
            }
        } catch (RuntimeException e) {
            LOGGER.warn("No se pudieron descubrir en Docker los contenedores administrados por DockIA Agents", e);
        }

        knownContainerIds.addAll(runtimesByContainer.keySet());
        return new ArrayList<>(runtimesByContainer.values());
    }

    private void addRuntime(Map<String, AgentRuntimeInfo> runtimesByContainer, AgentRuntimeInfo runtime) {
        if (runtime == null) return;
        String containerId = normalize(runtime.getContainerId());
        if (containerId == null) return;
        runtimesByContainer.putIfAbsent(containerId, runtime);
    }

    private AgentRuntimeInfo runtimeFromContainerId(String containerId) {
        AgentRuntimeInfo info = new AgentRuntimeInfo();
        info.setAgentId(containerId);
        info.setContainerId(containerId);
        return info;
    }

    private boolean isStartAgentsOnStart() {
        return Boolean.parseBoolean(getProperty("alfresco.aiagents.subsystem.startAgentsOnStart", "true"));
    }

    private boolean isStopAgentsOnStop() {
        return Boolean.parseBoolean(getProperty("alfresco.aiagents.subsystem.stopAgentsOnStop", "true"));
    }

    private int getStopTimeoutSeconds() {
        String raw = getProperty("alfresco.aiagents.subsystem.stopTimeoutSeconds", "10");
        try {
            return Math.max(0, Integer.parseInt(raw));
        } catch (NumberFormatException e) {
            return 10;
        }
    }

    private String getProperty(String key, String defaultValue) {
        if (globalProperties == null) {
            return defaultValue;
        }
        return globalProperties.getProperty(key, defaultValue);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * Escribe tambien en stdout porque es la salida que Docker incorpora de
     * forma incondicional al log del contenedor, aunque Log4j filtre la clase.
     */
    private void writeToContainerLog(String message) {
        System.out.println(message);
        System.out.flush();
    }
}
