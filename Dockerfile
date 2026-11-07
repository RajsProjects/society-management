# ── Stage 1: Build ──
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app

# Cache dependencies layer
COPY pom.xml .
RUN mvn dependency:go-offline -B -q

# Copy source and build executable JAR
COPY src ./src
RUN mvn clean package -DskipTests -B -q

# ── Stage 2: Runtime ──
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Add unprivileged system user and group
RUN addgroup -S spring && adduser -S spring -G spring

# Copy prebuilt jar with correct ownership
COPY --from=builder --chown=spring:spring /app/target/*.jar app.jar

USER spring

# Expose container port
EXPOSE 8080

# Default environment configuration for container environments
ENV PORT=8080 \
    SERVER_PORT=8080 \
    SPRING_PROFILES_ACTIVE=prod

# Container health check against Spring Boot Actuator
HEALTHCHECK --interval=30s --timeout=5s --start-period=45s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:${PORT:-8080}/actuator/health || exit 1

# Production JVM container flags (fail-fast on OOM to trigger orchestrator restart)
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:InitialRAMPercentage=50.0", \
    "-XX:+ExitOnOutOfMemoryError", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
