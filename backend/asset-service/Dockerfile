# Build stage
FROM eclipse-temurin:17-jdk AS builder

WORKDIR /build

# Copy gradle wrapper and config
COPY gradlew ./
COPY gradle ./gradle
COPY build.gradle.kts ./
COPY settings.gradle.kts ./
COPY gradle.properties ./

# Copy common module
COPY common ./common

# Copy asset-service
COPY asset-service ./asset-service

# Build
RUN chmod +x gradlew && ./gradlew :asset-service:bootJar -x test --no-daemon

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
