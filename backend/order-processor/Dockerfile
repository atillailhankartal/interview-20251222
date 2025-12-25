# Build stage
FROM gradle:8.5-jdk17 AS builder

WORKDIR /build

# Copy gradle files and common module
COPY build.gradle.kts settings.gradle.kts gradle.properties ./
COPY common ./common
COPY order-processor ./order-processor

# Build the application
RUN gradle :order-processor:bootJar -x test --no-daemon

# Runtime stage
FROM eclipse-temurin:17-jre

LABEL maintainer="brokage-team"
LABEL service="order-processor"

WORKDIR /app

RUN groupadd -r spring && useradd -r -g spring spring

COPY --from=builder /build/order-processor/build/libs/*-SNAPSHOT.jar app.jar

RUN chown -R spring:spring /app

USER spring

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8084

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
