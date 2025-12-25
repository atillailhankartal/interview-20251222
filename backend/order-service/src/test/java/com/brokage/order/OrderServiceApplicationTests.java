package com.brokage.order;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration test - requires running infrastructure (Kafka, Redis, Keycloak)
 * Run with: docker-compose up -d && ./gradlew :order-service:test
 */
@SpringBootTest
@ActiveProfiles("test")
@Disabled("Requires running infrastructure - use for integration testing only")
class OrderServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
