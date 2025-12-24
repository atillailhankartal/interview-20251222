# Build stage - Use gradle image to avoid download issues
FROM gradle:8.5-jdk17 AS builder

WORKDIR /build

# Copy gradle config
COPY build.gradle.kts settings.gradle.kts gradle.properties ./

# Copy common module
COPY common ./common

# Copy asset-service
COPY asset-service ./asset-service

# Build
RUN gradle :asset-service:bootJar -x test --no-daemon

# Runtime stage
FROM eclipse-temurin:17-jre

LABEL maintainer="brokage-team"
LABEL service="asset-service"

WORKDIR /app

RUN groupadd -r spring && useradd -r -g spring spring

COPY --from=builder /build/asset-service/build/libs/*-SNAPSHOT.jar app.jar

RUN chown -R spring:spring /app

USER spring

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8082

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
