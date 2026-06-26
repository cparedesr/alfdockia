# ALFDOCKIA

<p align="center">
  <img src="images/alfdockia.png" alt="Arquitectura">
</p>

Este proyecto añade a Alfresco Content Services (ACS) un modulo para usar
Alfresco como plano de control de agentes de IA desplegados como contenedores
Docker. Expone una API REST para crear, listar, consultar y eliminar agentes, y
guarda su definicion y estado dentro del repositorio de Alfresco para mantener
trazabilidad.

## Arquitectura

![Arquitectura](images/arquitectura.png)

## Organizacion del codigo

- `src/main/java/com/cparedesr/dockia/agents/webscripts`: endpoints REST de
  Alfresco Web Scripts.
- `src/main/java/com/cparedesr/dockia/agents/service`: casos de uso de
  validacion, despliegue y borrado.
- `src/main/java/com/cparedesr/dockia/agents/service/docker`: contrato e
  implementacion de integracion con Docker.
- `src/main/java/com/cparedesr/dockia/agents/service/registry`: persistencia
  del registro de agentes dentro del repositorio de Alfresco.
- `src/main/java/com/cparedesr/dockia/agents/service/repo`: resolucion y
  creacion de carpetas/nodos destino en Alfresco.
- `src/main/java/com/cparedesr/dockia/agents/service/secrets`: resolucion de
  secretos desde propiedades de Alfresco.
- `src/main/java/com/cparedesr/dockia/agents/service/subsystem`: integracion
  con el ciclo de vida de subsistemas de Alfresco.
- `src/main/java/com/cparedesr/dockia/agents/model`: DTOs usados por la API.
- `src/main/resources/alfresco/module/alfresco-dockia-agents`: configuracion,
  modelo de contenido y Spring context padre del modulo.
- `src/main/resources/alfresco/subsystems/DockIAAgents/default`: contexto hijo
  del subsistema donde se encapsulan los servicios de agentes.
- `agents/extract metadata`: agente Spring Boot out-of-process que escucha
  eventos de Alfresco, lee contenidos y escribe metadatos extraidos.

## Funcionamiento

Al desplegar un agente, Alfresco valida la configuracion, resuelve los
`secretRef` definidos en `alfresco-global.properties` y prepara las variables
de entorno para el contenedor: conexion a Alfresco, broker de eventos, tipo
documental que debe procesar y configuracion LLM. Los Web Scripts actuan como
fachada publica y delegan en el subsistema `DockIAAgents`, que encapsula
validacion, despliegue, registro, secretos, Docker y borrado.

Cuando Alfresco inicia el subsistema, se leen los agentes registrados y se
arrancan sus contenedores. Cuando el subsistema se detiene, se paran los
contenedores registrados con el timeout configurado. El despliegue de nuevos
contenedores se realiza por socket local; las operaciones de arranque, parada y
borrado soportan socket local o Docker Remote API cuando se configura
`mode=url`.

El agente queda registrado en `Repositorio > Data Dictionary > AI Agents` con
su `agentId`, `containerId`, estados deseado/actual y configuracion saneada. Los
secretos nunca se persisten en claro: solo se guarda la referencia al secreto.

## Configuracion

Si se usa Docker por socket, primero consulta el GID del socket en el host:

```bash
stat -c '%g' /var/run/docker.sock
```

Despues revisa estos archivos:

```text
docker/docker-compose.yml
src/main/docker/Dockerfile
```

El `docker-compose.yml` monta `/var/run/docker.sock` en el contenedor de ACS. El
`Dockerfile` instala el CLI de Docker y añade el usuario `alfresco` al grupo que
puede usar el socket.

Propiedades principales en `alfresco-global.properties`:

```properties
alfresco.aiagents.docker.enabled=true

alfresco.aiagents.subsystem.autoStart=true
alfresco.aiagents.subsystem.startAgentsOnStart=true
alfresco.aiagents.subsystem.stopAgentsOnStop=true
alfresco.aiagents.subsystem.stopTimeoutSeconds=10

alfresco.aiagents.docker.mode=socket
alfresco.aiagents.docker.socket=/var/run/docker.sock
alfresco.aiagents.docker.network=alfdockia

# Opcional si se usa Docker Remote API para operaciones soportadas.
# alfresco.aiagents.docker.baseUrl=https://dockerhost:2376
# alfresco.aiagents.docker.tls.keystore.path=/opt/alfresco/tls/docker-client.p12
# alfresco.aiagents.docker.tls.keystore.password=changeit
# alfresco.aiagents.docker.tls.truststore.path=/opt/alfresco/tls/docker-trust.p12
# alfresco.aiagents.docker.tls.truststore.password=changeit

alfresco.aiagents.secret.svc_ai_password=supersecret

alfresco.aiagents.image.allowlist.enabled=false
# alfresco.aiagents.image.allowlist=cparedes/agents/,ghcr.io/miorg/,registry.local/
```

## Construir el agente extract metadata

```bash
docker build -t cparedes/agents/extract-metadata:1.0.0 "agents/extract metadata"
```

## Crear un agente

Crear un agente despliega el contenedor y crea un nodo de registro en Alfresco:

```bash
curl -u admin:admin \
  -X POST "http://localhost:8080/alfresco/s/api/-default-/public/ai-agents/versions/1/agents" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "extract-metadata1",
    "image": "cparedes/agents/extract-metadata:1.0.0",
    "ports": [
      { "containerPort": 8080, "hostPort": 9090, "protocol": "tcp" }
    ],
    "alfresco": {
      "baseUrl": "http://alfresco-dockia-agents-acs:8080/alfresco",
      "authType": "basic",
      "username": "svc_ai",
      "passwordSecretRef": {
        "secretRef": "prop:alfresco.aiagents.secret.svc_ai_password"
      },
      "documentType": "cm:content",
      "eventsBrokerUrl": "tcp://alfresco-dockia-agents-activemq:61616"
    },
    "llm": {
      "provider": "ollama",
      "baseUrl": "http://ollama:11434",
      "model": "llama3.1",
      "prompt": "Eres un agente que extrae nombre, apellido1, apellido2, DNI y fechaNacimiento de documentos. Devuelve solo JSON valido."
    },
    "env": {
      "LOG_LEVEL": "info"
    }
  }'
```

La contraseña se resuelve con la propiedad configurada en
`alfresco-global.properties`.

## Operaciones utiles

Comprobar que el contenedor recibio la contraseña:

```bash
docker inspect <container_id> \
  --format '{{range .Config.Env}}{{println .}}{{end}}' | grep ALFRESCO_PASSWORD
```

Listar agentes:

```bash
curl -u admin:admin \
  -H "Accept: application/json" \
  "http://localhost:8080/alfresco/s/api/-default-/public/ai-agents/versions/1/agents"
```

Consultar un agente:

```bash
curl -u admin:admin \
  -H "Accept: application/json" \
  "http://localhost:8080/alfresco/s/api/-default-/public/ai-agents/versions/1/agents/agent-6c00f331-342f-4658-9d00-02f3a7d0366a"
```

Eliminar un agente:

```bash
curl -u admin:admin -X DELETE \
  "http://localhost:8080/alfresco/s/api/-default-/public/ai-agents/versions/1/agents/agent-6c00f331-342f-4658-9d00-02f3a7d0366a"
```

## Variables de entorno del agente

Al desplegar un agente, Alfresco inyecta configuracion mediante variables de
entorno.

**Alfresco**

- `ALFRESCO_BASE_URL`
- `ALFRESCO_AUTH_TYPE` (`basic`, `bearer` o `none`)
- `ALFRESCO_USERNAME`
- `ALFRESCO_PASSWORD` resuelto desde `secretRef`
- `ALFRESCO_DOCUMENT_TYPE`
- `ALFRESCO_EVENTS_BROKER_URL`

**LLM**

- `LLM_PROVIDER`
- `LLM_BASE_URL`
- `LLM_MODEL`
- `LLM_API_KEY` resuelto desde `secretRef`, si aplica
- `AGENT_PROMPT`

**Extras**

Las variables definidas en el bloque `env` del JSON del agente, por ejemplo
`LOG_LEVEL=info`.

La preparacion de estas variables vive en
`src/main/java/com/cparedesr/dockia/agents/service/AgentDeploymentService.java`.
