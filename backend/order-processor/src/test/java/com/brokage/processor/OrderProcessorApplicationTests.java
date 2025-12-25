package com.brokage.processor;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration test - requires running infrastructure (PostgreSQL, Kafka, Redis, Keycloak)
 * Run with: docker-compose up -d && ./gradlew :order-processor:test
 */
@SpringBootTest
@ActiveProfiles("test")
@Disabled("Requires running infrastructure - use for integration testing only")
class OrderProcessorApplicationTests {

    @Test
    void contextLoads() {
    }
}
