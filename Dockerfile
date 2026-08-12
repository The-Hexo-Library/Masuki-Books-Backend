# syntax=docker/dockerfile:1

# =========================
# Build stage
# =========================
FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /app

# Copy Maven configuration first for better Docker layer caching
COPY backend/pom.xml ./

# Download dependencies
RUN mvn -q -DskipTests dependency:go-offline

# Copy backend source
COPY backend/src ./src

# Build the application
RUN mvn -q -DskipTests clean package


# =========================
# Runtime stage
# =========================
FROM eclipse-temurin:21-jre-jammy AS runtime

WORKDIR /app

# Install required packages and create non-root user
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl ca-certificates tzdata \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system spring \
    && useradd --system --gid spring --create-home --home-dir /home/spring spring

# Render provides PORT automatically.
# 8081 is used locally if PORT is not provided.
ENV TZ=UTC \
    SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS=""

# Copy the built JAR
COPY --from=builder /app/target/*.jar /app/app.jar

# Documentation/default port
EXPOSE 8081

# Run as non-root user
USER spring:spring

# Health check
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=5 \
    CMD curl -fsS http://localhost:${PORT:-8081}/actuator/health || exit 1

# Start application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]