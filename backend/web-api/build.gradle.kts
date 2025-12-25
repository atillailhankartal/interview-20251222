dependencies {
    implementation(project(":common"))

    // WebFlux for SSE support
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    // Swagger/OpenAPI for WebFlux (overrides webmvc version from root)
    implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:2.3.0")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // Security
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")

    // Redis for caching
    implementation("org.springframework.boot:spring-boot-starter-data-redis-reactive")

    // Kafka for consuming events
    implementation("org.springframework.kafka:spring-kafka")
    implementation("io.confluent:kafka-avro-serializer:7.5.0")

    // Observability
    implementation("io.micrometer:micrometer-tracing-bridge-otel")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp")
    implementation("com.github.loki4j:loki-logback-appender:1.4.2")

    // Test
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("io.projectreactor:reactor-test")
}
