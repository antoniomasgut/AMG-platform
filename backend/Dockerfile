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
  CMD wget -qO- http://127.0.0.1:8080/api/v1/ops/health >/dev/null 2>&1 || exit 1

ENTRYPOINT ["java", "-Djava.net.preferIPv4Stack=true", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
