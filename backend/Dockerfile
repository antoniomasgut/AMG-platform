# --- Fase 1: build ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Descàrrega de dependències en capa separada (aprofita la caché de Docker)
COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn package -B -DskipTests

# --- Fase 2: runtime ---
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

RUN addgroup -S amg && adduser -S amg -G amg

COPY --from=build /app/target/*.jar app.jar

RUN chown amg:amg app.jar
USER amg

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
