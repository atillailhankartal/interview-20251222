FROM eclipse-temurin:17-jre

LABEL maintainer="brokage-team"
LABEL service="notification-service"

WORKDIR /app

RUN groupadd -r spring && useradd -r -g spring spring

COPY build/libs/*-SNAPSHOT.jar app.jar

RUN chown -R spring:spring /app

USER spring

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8085

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
