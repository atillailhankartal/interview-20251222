package com.brokage.asset;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration test - requires running infrastructure (Kafka, Redis, PostgreSQL)
 * Run with: docker-compose up -d && ./gradlew :asset-service:test
 */
@SpringBootTest
@ActiveProfiles("test")
@Disabled("Requires running infrastructure - use for integration testing only")
class AssetServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
