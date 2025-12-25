package com.brokage.integration;

import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

/**
 * Base class for integration tests.
 *
 * These tests are designed to run against already running services.
 * Prerequisites:
 * - All services must be running via docker-compose
 * - Infrastructure (Postgres, MongoDB, Kafka) must be available
 * - Keycloak must be running at localhost:8180
 *
 * Default service URLs (can be overridden via environment variables):
 * - ORDER_SERVICE_URL: http://localhost:7081
 * - ASSET_SERVICE_URL: http://localhost:7082
 * - CUSTOMER_SERVICE_URL: http://localhost:7083
 * - KEYCLOAK_URL: http://localhost:8180
 */
public abstract class BaseIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(BaseIntegrationTest.class);

    // Service URLs
    protected static String orderServiceUrl;
    protected static String assetServiceUrl;
    protected static String customerServiceUrl;
    protected static String kafkaBootstrapServers;
    protected static String keycloakUrl;

    // Keycloak credentials
    protected static final String CLIENT_ID = "brokage-api";
    protected static final String CLIENT_SECRET = "brokage-api-secret";
    protected static final String REALM = "brokage";

    // Test users (passwords from realm-export.json)
    protected static final String ADMIN_USERNAME = "admin";
    protected static final String ADMIN_PASSWORD = "admin123";
    protected static final String CUSTOMER_USERNAME = "customer1";
    protected static final String CUSTOMER_PASSWORD = "customer123";
    protected static final String BROKER_USERNAME = "broker1";
    protected static final String BROKER_PASSWORD = "broker123";

    // Token cache
    private static final Map<String, String> tokenCache = new HashMap<>();

    protected String authToken;

    @BeforeAll
    static void setUpInfrastructure() {
        // Use running services from docker-compose (ports 7081, 7082, 7083)
        orderServiceUrl = System.getenv().getOrDefault("ORDER_SERVICE_URL", "http://localhost:7081");
        assetServiceUrl = System.getenv().getOrDefault("ASSET_SERVICE_URL", "http://localhost:7082");
        customerServiceUrl = System.getenv().getOrDefault("CUSTOMER_SERVICE_URL", "http://localhost:7083");
        kafkaBootstrapServers = System.getenv().getOrDefault("KAFKA_BOOTSTRAP_SERVERS", "localhost:9093");
        keycloakUrl = System.getenv().getOrDefault("KEYCLOAK_URL", "http://localhost:8180");

        log.info("Order Service URL: {}", orderServiceUrl);
        log.info("Asset Service URL: {}", assetServiceUrl);
        log.info("Customer Service URL: {}", customerServiceUrl);
        log.info("Kafka Bootstrap Servers: {}", kafkaBootstrapServers);
        log.info("Keycloak URL: {}", keycloakUrl);
    }

    @BeforeEach
    void setUp() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        // Configure RestAssured to not throw exceptions on error codes
        RestAssured.config = RestAssuredConfig.config()
                .httpClient(HttpClientConfig.httpClientConfig()
                        .dontReuseHttpClientInstance());
        authToken = getAdminToken();
    }

    /**
     * Gets infrastructure connection details.
     */
    protected static String getKafkaBootstrapServers() {
        return kafkaBootstrapServers;
    }

    /**
     * Gets an admin authentication token from Keycloak.
     */
    protected String getAdminToken() {
        return getToken(ADMIN_USERNAME, ADMIN_PASSWORD);
    }

    /**
     * Gets a customer authentication token from Keycloak.
     */
    protected String getCustomerToken() {
        return getToken(CUSTOMER_USERNAME, CUSTOMER_PASSWORD);
    }

    /**
     * Gets a broker authentication token from Keycloak.
     */
    protected String getBrokerToken() {
        return getToken(BROKER_USERNAME, BROKER_PASSWORD);
    }

    /**
     * Gets an authentication token from Keycloak for the specified user.
     */
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
            log.error("Error getting token from Keycloak", e);
            throw new RuntimeException("Failed to get token from Keycloak", e);
        }
    }

    /**
     * Gets a test authentication token (uses admin token by default).
     * @deprecated Use getAdminToken(), getCustomerToken(), or getBrokerToken() instead.
     */
    @Deprecated
    protected String getTestToken() {
        return getAdminToken();
    }

    /**
     * Helper to create an order via REST API.
     * Uses extract().response() to prevent RestAssured from throwing on error codes.
     */
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

    /**
     * Helper to get an order by ID.
     * Configures RestAssured to accept any status code.
     */
    protected Response getOrder(String orderId) {
        return given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken)
                .expect()
                .statusCode(greaterThanOrEqualTo(0))  // Accept any status code
                .when()
                .get(orderServiceUrl + "/api/orders/" + orderId);
    }

    /**
     * Helper to cancel an order.
     * Configures RestAssured to accept any status code.
     */
    protected Response cancelOrder(String orderId) {
        return given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken)
                .expect()
                .statusCode(greaterThanOrEqualTo(0))  // Accept any status code
                .when()
                .delete(orderServiceUrl + "/api/orders/" + orderId);
    }

    /**
     * Helper to get customer assets.
     * Uses extract().response() to prevent RestAssured from throwing on error codes.
     */
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

    /**
     * Helper to deposit money.
     * Uses extract().response() to prevent RestAssured from throwing on error codes.
     */
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

    /**
     * Helper to withdraw money.
     * Uses extract().response() to prevent RestAssured from throwing on error codes.
     */
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
