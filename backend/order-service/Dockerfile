FROM eclipse-temurin:17-jre

LABEL maintainer="brokage-team"
LABEL service="order-service"

WORKDIR /app

# Add a non-root user
RUN groupadd -r spring && useradd -r -g spring spring

# Copy the jar file (exclude plain jar)
COPY build/libs/*-SNAPSHOT.jar app.jar

# Set ownership
RUN chown -R spring:spring /app

USER spring

# JVM options for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8081

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
