# Etapa 1: Build
FROM maven:3.9.12-eclipse-temurin-25 AS build
# Establecemos el directorio de trabajo
WORKDIR /app
# Copiamos el pom.xml y descargamos las dependencias para aprovechar la cache de Docker
COPY pom.xml .
RUN mvn dependency:go-offline -B
# Copiamos el código fuente y generamos el JAR
COPY src ./src
RUN mvn clean package -DskipTests

# Etapa 2: Runtime
FROM eclipse-temurin:25-jre-jammy
WORKDIR /app
# Instalamos curl para el healthcheck de Docker
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*
# Creamos un usuario sin privilegios por seguridad
RUN addgroup --system spring && adduser --system spring --ingroup spring
# Usamos el usuario creado
USER spring
# Copiamos el JAR generado
COPY --from=build /app/target/gateway-*.jar app.jar
EXPOSE 8080
# Arrancamos la aplicación con el perfil de docker
ENTRYPOINT ["java", "-Dspring.profiles.active=docker", "-jar", "app.jar"]