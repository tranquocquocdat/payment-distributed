# Multi-stage build for Payment System
# --- Build stage ---
FROM maven:3.9.8-eclipse-temurin-17 AS build

WORKDIR /build

# Copy pom.xml first for better Docker layer caching
COPY pom.xml .

# Download dependencies (cached if pom.xml unchanged)
RUN mvn -B -q -DskipTests dependency:go-offline

# Copy source code
COPY src ./src

# Build the application
RUN mvn -B -DskipTests clean package

# --- Runtime stage ---
FROM eclipse-temurin:17-jre

# Install curl for health check
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy the built jar from build stage
COPY --from=build /build/target/*.jar app.jar

# Create non-root user
RUN groupadd -r payment && useradd -r -g payment payment
RUN chown -R payment:payment /app
USER payment

# Expose container port 8080
EXPOSE 8080

# Health check inside container
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -fsS http://localhost:8080/api/v1/actuator/health || exit 1

# JVM options (container-aware)
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
