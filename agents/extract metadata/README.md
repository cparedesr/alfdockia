# Extract Metadata Agent

Agente Spring Boot out-of-process para Alfresco. Escucha eventos de creacion de
ficheros, filtra por el tipo documental configurado y extrae estos metadatos
con Ollama:

- `em:nombre`
- `em:apellido1`
- `em:apellido2`
- `em:dni`
- `em:fechaNacimiento`

## Construir imagen

```bash
docker build -t cparedes/agents/extract-metadata:1.0.0 "agents/extract metadata"
```

## Variables principales

- `ALFRESCO_BASE_URL`
- `ALFRESCO_AUTH_TYPE`
- `ALFRESCO_USERNAME`
- `ALFRESCO_PASSWORD`
- `ALFRESCO_DOCUMENT_TYPE`
- `ALFRESCO_EVENTS_BROKER_URL`
- `LLM_PROVIDER`
- `LLM_BASE_URL`
- `LLM_MODEL`
- `AGENT_PROMPT`
