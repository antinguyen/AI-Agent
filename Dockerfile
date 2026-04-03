# ─────────────────────────────────────────────
# Stage 1: Build
# ─────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-jammy AS builder

# Install Maven
RUN apt-get update \
    && apt-get install -y --no-install-recommends maven \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /workspace

# Cache dependency layer separately
COPY pom.xml .
RUN mvn dependency:go-offline -B -q

# Build application
COPY src ./src
RUN mvn package -DskipTests -B -q

# ─────────────────────────────────────────────
# Stage 2: Runtime
# ─────────────────────────────────────────────
FROM eclipse-temurin:21-jre-jammy

# Create non-root user for security
RUN groupadd --system --gid 1001 appgroup \
    && useradd --system --uid 1001 --gid appgroup appuser

WORKDIR /app

COPY --from=builder /workspace/target/*.jar app.jar
RUN mkdir -p /app/uploads/products \
    && chown -R appuser:appgroup /app

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
