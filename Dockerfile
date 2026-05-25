# ── Stage 1: Build ─────────────────────────────────────────────────────────────
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy the POM first so the dependency layer is cached separately from source.
# Dependencies are only re-downloaded when pom.xml changes.
COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn package -DskipTests -B

# ── Stage 2: Runtime ───────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8081

# UseContainerSupport  — JVM respects the container memory limit (not host RAM).
# MaxRAMPercentage=75  — cap the heap at 75% of the container's available memory.
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-jar", "app.jar"]
