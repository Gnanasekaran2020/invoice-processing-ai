# =============================================================
#  Invoice Processing System — Spring Boot API
#  Multi-stage build: Maven build → JRE runtime
#  Java 21 | Spring Boot 3.3 | artifact: invoice-processing-system-1.0.0.jar
# =============================================================

# ── Stage 1: Build ────────────────────────────────────────────
FROM maven:3.9.7-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# Copy dependency manifests first for Docker layer caching
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Copy source and build (skip tests — run separately in CI)
COPY src ./src
RUN mvn clean package -DskipTests -q

# ── Stage 2: Runtime ──────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime

# Security: run as non-root user
RUN addgroup -S invoice && adduser -S invoice -G invoice

WORKDIR /app

# Copy built JAR from builder stage
COPY --from=builder /build/target/invoice-processing-system-1.0.0.jar app.jar

# Give ownership to non-root user
RUN chown invoice:invoice app.jar

USER invoice

# Expose Spring Boot default port
EXPOSE 8080

# Health check — uses Spring Actuator
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

# JVM flags tuned for containerised environment
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:+UseG1GC", \
  "-XX:+OptimizeStringConcat", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:default}", \
  "-jar", "app.jar"]

