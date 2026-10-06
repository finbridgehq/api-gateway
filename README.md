# Vankoo API Gateway

Este microservicio es el punto de entrada unificado para todas las solicitudes externas a la arquitectura de **Vankoo**. Utiliza **Spring Cloud Gateway** para enrutar peticiones a los servicios internos (IAM, Invoicing, Finance, etc.) desde una única URL y puerto propio.
Se encarga de manejar la autenticación (validando JWT emitidos por el IAM), aplicar políticas de seguridad y realizar balanceo de carga entre instancias de los servicios internos.

Es reactivo (utiliza WebFlux) para manejar eficientemente múltiples conexiones simultáneas, lo que es ideal para un Gateway que puede recibir un alto volumen de tráfico.

Idealmente, los demás servicios no deberían ser accesibles directamente desde el exterior, sino solo a través de este Gateway.

## Requisitos Previos

* **Java:** 25 (Temurin recomendado)
* **Maven:** 3.9+
* **Docker & Docker Compose**

## Instalación y Compilación

Para generar el artefacto ejecutable (JAR), corre el siguiente comando en la raíz de este proyecto:

```bash
mvn clean package -DskipTests
```

También puedes hacerlo usando la interfaz de IntelliJ:

1. Abre el proyecto en IntelliJ.
2. Navega a la pestaña "Maven" en el panel lateral derecho.
3. Expande el proyecto y luego la sección "Lifecycle".
4. Haz doble clic en "clean" para limpiar el proyecto.
5. Haz doble clic en "package" para compilar y empaquetar el proyecto. Puedes usar el botón "Toggle Skip Tests" para omitir las pruebas si lo deseas.

Independientemente de la opción elegida, el archivo generado se ubicará en `target/gateway-0.0.1-SNAPSHOT.jar`.

## Ejecución Local

Puedes ejecutar el servicio localmente desde IntelliJ, pero su configuración está optimizada para correr dentro de Docker. Si deseas ejecutarlo localmente, asegúrate de configurar el perfil `dev` para cargar la configuración adecuada

## Uso con Docker

Este servicio está diseñado para integrarse con el repositorio `vankoo-infra`.

1. Si ya tenías contenedores de vankoo-infra, levanta solo el Discovery Server en el repositorio de infraestructura:

    ```bash
    docker compose up -d --build api-gateway
    ```

2. Si es la primera vez que levantas vankoo-infra, simplemente corre en vankoo-infra:

    ```bash
    docker compose up -d --build
    ```

## Endpoints de Utilidad

Una vez iniciado, puedes acceder a:

- **Monitorización:** `http://localhost:8080/actuator` para ver el monitor de Actuator y verificar el estado del Gateway.
- **Health Check:** `http://localhost:8761/actuator/health` para verificar que el servicio esté funcionando correctamente.
- **Rutas de Enrutamiento:** `http://localhost:8080/actuator/gateway/routes` para ver las rutas configuradas en el Gateway.
- **Documentación de APIs:** `http://localhost:8080/scalar` para acceder a la documentación interactiva de las APIs expuestas a través del Gateway.

## Perfiles de Configuración

El proyecto utiliza perfiles para separar entornos:

- **dev:** Configuración para desarrollo local.
- **docker:** Configuración optimizada para correr dentro de Docker, con ajustes de red y puertos.

## Notas de Desarrollo

- **Seguridad:** El Gateway valida los JWT, pero solo el IAM puede emitirlos. Los demás servicios confían en el Gateway para la autenticación, por lo que es crucial mantener esta configuración segura y actualizada.
- **Inyección de Encabezados:** El Gateway lee el token, extrae su información y la inyecta en los encabezados de las solicitudes que envía a los servicios internos. Esto permite que los servicios internos puedan identificar al usuario sin necesidad de validar el token por sí mismos, evitando acoplamiento entre servicios.
- **Enrutamiento:** Las rutas están definidas en `application.yaml` bajo la sección `spring.cloud.gateway.routes`. Asegúrate de mantener esta configuración actualizada a medida que agregas nuevos servicios o cambias las rutas existentes.
- **Documentación de APIs:** Se ha integrado documentación en Scalar, la cual se puede ampliar agregando más en la lista de `sources` de `application.yaml`. Esto es independiente a las `routes` del Gateway, pero es importante mantener ambas configuraciones actualizadas para asegurar que la documentación refleje correctamente las APIs expuestas.

## Información Adicional

Puedes encontrar más detalles sobre el proyecto en el directorio de [Documentación](/docs), donde se agregaron guías en markdown y demás documentación relevante para el desarrollo y mantenimiento del servicio.