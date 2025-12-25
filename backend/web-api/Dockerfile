# Build stage - using Gradle image with Gradle pre-installed
FROM gradle:8.5-jdk17 AS builder

WORKDIR /build

# Copy gradle config
COPY build.gradle.kts ./
COPY settings.gradle.kts ./
COPY gradle.properties ./

# Copy common module
COPY common ./common

# Copy web-api service
COPY web-api ./web-api

# Build
RUN gradle :web-api:bootJar -x test --no-daemon

# Runtime stage
FROM eclipse-temurin:17-jre

LABEL maintainer="brokage-team"
LABEL service="web-api"

WORKDIR /app

RUN groupadd -r spring && useradd -r -g spring spring

COPY --from=builder /build/web-api/build/libs/*-SNAPSHOT.jar app.jar

RUN chown -R spring:spring /app

USER spring

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8087

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
