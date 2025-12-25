package com.brokage.audit;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration test - requires running infrastructure (MongoDB, Kafka, Keycloak)
 * Run with: docker-compose up -d && ./gradlew :audit-service:test
 */
@SpringBootTest
@ActiveProfiles("test")
@Disabled("Requires running infrastructure - use for integration testing only")
class AuditServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
