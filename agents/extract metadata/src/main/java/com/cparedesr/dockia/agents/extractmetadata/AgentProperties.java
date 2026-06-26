/*
 * Copyright (c) 2026 cparedes. Todos los derechos reservados.
 */
package com.cparedesr.dockia.agents.extractmetadata;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "agent")
public class AgentProperties {

    private String alfrescoBaseUrl;
    private String alfrescoAuthType = "basic";
    private String alfrescoUsername;
    private String alfrescoPassword;
    private String documentType = "cm:content";
    private String metadataAspect = "em:personMetadata";
    private int contentMaxChars = 12000;
    private Llm llm = new Llm();

    public String getAlfrescoBaseUrl() {
        return alfrescoBaseUrl;
    }

    public void setAlfrescoBaseUrl(String alfrescoBaseUrl) {
        this.alfrescoBaseUrl = trimTrailingSlash(alfrescoBaseUrl);
    }

    public String getAlfrescoAuthType() {
        return alfrescoAuthType;
    }

    public void setAlfrescoAuthType(String alfrescoAuthType) {
        this.alfrescoAuthType = alfrescoAuthType;
    }

    public String getAlfrescoUsername() {
        return alfrescoUsername;
    }

    public void setAlfrescoUsername(String alfrescoUsername) {
        this.alfrescoUsername = alfrescoUsername;
    }

    public String getAlfrescoPassword() {
        return alfrescoPassword;
    }

    public void setAlfrescoPassword(String alfrescoPassword) {
        this.alfrescoPassword = alfrescoPassword;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public String getMetadataAspect() {
        return metadataAspect;
    }

    public void setMetadataAspect(String metadataAspect) {
        this.metadataAspect = metadataAspect;
    }

    public int getContentMaxChars() {
        return contentMaxChars;
    }

    public void setContentMaxChars(int contentMaxChars) {
        this.contentMaxChars = contentMaxChars;
    }

    public Llm getLlm() {
        return llm;
    }

    public void setLlm(Llm llm) {
        this.llm = llm;
    }

    private String trimTrailingSlash(String value) {
        return value == null ? null : value.replaceAll("/+$", "");
    }

    public static class Llm {
        private String provider = "ollama";
        private String baseUrl;
        private String model;
        private String apiKey;
        private String prompt;

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl == null ? null : baseUrl.replaceAll("/+$", "");
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getPrompt() {
            return prompt;
        }

        public void setPrompt(String prompt) {
            this.prompt = prompt;
        }
    }
}
