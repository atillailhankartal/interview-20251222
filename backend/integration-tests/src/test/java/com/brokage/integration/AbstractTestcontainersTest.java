package com.brokage.integration;

import com.brokage.integration.config.TestcontainersConfig;
import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

/**
 * Abstract base class for integration tests using Testcontainers.
 *
 * This class manages isolated containers for:
 * - PostgreSQL (with seed data)
 * - MongoDB (with seed data)
 * - Kafka
 * - Keycloak (with realm import)
 *
 * All containers use the same seed data as docker-compose,
 * ensuring stable IDs for testing.
 *
 * Note: This tests the services running via docker-compose,
 * but the infrastructure is isolated in Testcontainers.
 * For fully isolated tests, services would also need to run in Testcontainers.
 */
@Testcontainers
public abstract class AbstractTestcontainersTest {

    private static final Logger log = LoggerFactory.getLogger(AbstractTestcontainersTest.class);

    // =========================================================================
    // Testcontainers - Infrastructure
    // =========================================================================

    @Container
    protected static final PostgreSQLContainer<?> postgres = TestcontainersConfig.createPostgresContainer();

    @Container
    protected static final MongoDBContainer mongodb = TestcontainersConfig.createMongoContainer();

    @Container
    protected static final KafkaContainer kafka = TestcontainersConfig.createKafkaContainer();

    @Container
    protected static final GenericContainer<?> keycloak = TestcontainersConfig.createKeycloakContainer();

    // =========================================================================
    // Service URLs - Dynamic based on Testcontainers
    // =========================================================================

    protected static String keycloakUrl;
    protected static String postgresJdbcUrl;
    protected static String mongoConnectionString;
    protected static String kafkaBootstrapServers;

    // Service URLs (these still connect to docker-compose services for now)
    // In a fully isolated setup, these would also be Testcontainers
    protected static String orderServiceUrl;
    protected static String assetServiceUrl;
    protected static String customerServiceUrl;

    // =========================================================================
    // Keycloak Configuration
    // =========================================================================

    protected static final String CLIENT_ID = "brokage-api";
    protected static final String CLIENT_SECRET = "brokage-api-secret";
    protected static final String REALM = "brokage";

    // Test users from realm-export.json
    protected static final String ADMIN_USERNAME = "admin";
    protected static final String ADMIN_PASSWORD = "admin123";
    protected static final String CUSTOMER_USERNAME = "customer1";
    protected static final String CUSTOMER_PASSWORD = "customer123";
    protected static final String BROKER_USERNAME = "broker1";
    protected static final String BROKER_PASSWORD = "broker123";

    // Token cache
    private static final Map<String, String> tokenCache = new HashMap<>();

    protected String authToken;

    // =========================================================================
    // Known IDs from Seed Data (init-multiple-dbs.sh)
    // UUID Pattern: {role}0000001-0001-0001-0001-{sequence}
    //   a = Admin, b = Broker, c = Customer
    // =========================================================================

    // Admin (Nick Fury)
    protected static final String ADMIN_ID = "a0000001-0001-0001-0001-000000000001";

    // Brokers
    protected static final String BROKER_TONY_STARK_ID = "b0000001-0001-0001-0001-000000000001";
    protected static final String BROKER_STEVE_ROGERS_ID = "b0000001-0001-0001-0001-000000000002";
    protected static final String BROKER_GANDALF_ID = "b0000001-0001-0001-0001-000000000003";
    protected static final String BROKER_MORPHEUS_ID = "b0000001-0001-0001-0001-000000000005";
    protected static final String BROKER_DUMBLEDORE_ID = "b0000001-0001-0001-0001-000000000006";
    protected static final String BROKER_BRUCE_WAYNE_ID = "b0000001-0001-0001-0001-000000000008";

    // Marvel Customers (1-25)
    protected static final String PETER_PARKER_ID = "c0000001-0001-0001-0001-000000000001";
    protected static final String BRUCE_BANNER_ID = "c0000001-0001-0001-0001-000000000002";
    protected static final String THOR_ODINSON_ID = "c0000001-0001-0001-0001-000000000003";
    protected static final String NATASHA_ROMANOFF_ID = "c0000001-0001-0001-0001-000000000004";
    protected static final String TCHALLA_ID = "c0000001-0001-0001-0001-000000000010";
    protected static final String PEPPER_POTTS_ID = "c0000001-0001-0001-0001-000000000021";
    protected static final String THANOS_ID = "c0000001-0001-0001-0001-000000000024";

    // LOTR Customers (26-45)
    protected static final String FRODO_BAGGINS_ID = "c0000001-0001-0001-0001-000000000026";
    protected static final String SAMWISE_GAMGEE_ID = "c0000001-0001-0001-0001-000000000027";
    protected static final String GALADRIEL_ID = "c0000001-0001-0001-0001-000000000037";

    // Matrix Customers (46-55)
    protected static final String NEO_ANDERSON_ID = "c0000001-0001-0001-0001-000000000046";
    protected static final String TRINITY_ID = "c0000001-0001-0001-0001-000000000047";

    // Harry Potter Customers (56-80)
    protected static final String HARRY_POTTER_ID = "c0000001-0001-0001-0001-000000000056";
    protected static final String HERMIONE_GRANGER_ID = "c0000001-0001-0001-0001-000000000057";

    // DC Customers (81-100)
    protected static final String BARRY_ALLEN_ID = "c0000001-0001-0001-0001-000000000081";
    protected static final String LEX_LUTHOR_ID = "c0000001-0001-0001-0001-000000000094";

    // Aliases for common test scenarios
    protected static final String CUSTOMER1_ID = PETER_PARKER_ID;  // Standard tier
    protected static final String CUSTOMER2_ID = BRUCE_BANNER_ID;  // Premium tier
    protected static final String VIP_CUSTOMER_ID = THOR_ODINSON_ID;  // VIP tier

    // =========================================================================
    // Setup
    // =========================================================================

    @BeforeAll
    static void setUpContainers() {
        // Get URLs from Testcontainers
        keycloakUrl = TestcontainersConfig.getKeycloakUrl(keycloak);
        postgresJdbcUrl = TestcontainersConfig.getPostgresJdbcUrl(postgres);
        mongoConnectionString = TestcontainersConfig.getMongoConnectionString(mongodb);
        kafkaBootstrapServers = TestcontainersConfig.getKafkaBootstrapServers(kafka);

        // For now, services still run via docker-compose
        // TODO: In fully isolated setup, start services in Testcontainers too
        orderServiceUrl = System.getenv().getOrDefault("ORDER_SERVICE_URL", "http://localhost:7081");
        assetServiceUrl = System.getenv().getOrDefault("ASSET_SERVICE_URL", "http://localhost:7082");
        customerServiceUrl = System.getenv().getOrDefault("CUSTOMER_SERVICE_URL", "http://localhost:7083");

        log.info("=== Testcontainers Infrastructure ===");
        log.info("PostgreSQL JDBC URL: {}", postgresJdbcUrl);
        log.info("MongoDB Connection: {}", mongoConnectionString);
        log.info("Kafka Bootstrap: {}", kafkaBootstrapServers);
        log.info("Keycloak URL: {}", keycloakUrl);
        log.info("");
        log.info("=== Service URLs ===");
        log.info("Order Service: {}", orderServiceUrl);
        log.info("Asset Service: {}", assetServiceUrl);
        log.info("Customer Service: {}", customerServiceUrl);
    }

    @BeforeEach
    void setUp() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        RestAssured.config = RestAssuredConfig.config()
                .httpClient(HttpClientConfig.httpClientConfig()
                        .dontReuseHttpClientInstance());
        authToken = getAdminToken();
    }

    // =========================================================================
    // Token Methods
    // =========================================================================

    protected String getAdminToken() {
        return getToken(ADMIN_USERNAME, ADMIN_PASSWORD);
    }

    protected String getCustomerToken() {
        return getToken(CUSTOMER_USERNAME, CUSTOMER_PASSWORD);
    }

    protected String getBrokerToken() {
        return getToken(BROKER_USERNAME, BROKER_PASSWORD);
    }

    protected String getToken(String username, String password) {
        String cacheKey = username + ":" + password;
        if (tokenCache.containsKey(cacheKey)) {
            return tokenCache.get(cacheKey);
        }

        String tokenUrl = keycloakUrl + "/realms/" + REALM + "/protocol/openid-connect/token";

        try {
            Response response = given()
                    .contentType("application/x-www-form-urlencoded")
                    .formParam("client_id", CLIENT_ID)
                    .formParam("client_secret", CLIENT_SECRET)
                    .formParam("username", username)
                    .formParam("password", password)
                    .formParam("grant_type", "password")
                    .when()
                    .post(tokenUrl);

            if (response.statusCode() == 200) {
                String token = response.jsonPath().getString("access_token");
                tokenCache.put(cacheKey, token);
                log.info("Successfully obtained token for user: {}", username);
                return token;
            } else {
                log.error("Failed to get token for user {}: {} - {}",
                        username, response.statusCode(), response.body().asString());
                throw new RuntimeException("Failed to get token from Keycloak");
            }
        } catch (Exception e) {
            log.error("Error getting token from Keycloak at {}", tokenUrl, e);
            throw new RuntimeException("Failed to get token from Keycloak", e);
        }
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    protected Response createOrder(String customerId, String assetName,
                                   String orderSide, String price, String size) {
        Map<String, Object> orderRequest = Map.of(
                "customerId", customerId,
                "assetName", assetName,
                "orderSide", orderSide,
                "orderType", "LIMIT",
                "price", price,
                "size", size
        );

        return given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken)
                .body(orderRequest)
                .when()
                .post(orderServiceUrl + "/api/orders")
                .then()
                .extract()
                .response();
    }

    protected Response getOrder(String orderId) {
        return given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken)
                .expect()
                .statusCode(greaterThanOrEqualTo(0))
                .when()
                .get(orderServiceUrl + "/api/orders/" + orderId);
    }

    protected Response cancelOrder(String orderId) {
        return given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken)
                .expect()
                .statusCode(greaterThanOrEqualTo(0))
                .when()
                .delete(orderServiceUrl + "/api/orders/" + orderId);
    }

    protected Response getCustomerAssets(String customerId) {
        return given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken)
                .when()
                .get(assetServiceUrl + "/api/assets?customerId=" + customerId)
                .then()
                .extract()
                .response();
    }

    protected Response deposit(String customerId, String assetName, String amount) {
        Map<String, Object> request = Map.of(
                "customerId", customerId,
                "assetName", assetName,
                "amount", amount
        );

        return given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken)
                .body(request)
                .when()
                .post(assetServiceUrl + "/api/assets/deposit")
                .then()
                .extract()
                .response();
    }

    protected Response withdraw(String customerId, String assetName, String amount) {
        Map<String, Object> request = Map.of(
                "customerId", customerId,
                "assetName", assetName,
                "amount", amount
        );

        return given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken)
                .body(request)
                .when()
                .post(assetServiceUrl + "/api/assets/withdraw")
                .then()
                .extract()
                .response();
    }
}
