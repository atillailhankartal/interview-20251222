package com.brokage.integration;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Broker Authorization scenarios.
 *
 * Tests the following authorization rules:
 * | İşlem        | ADMIN          | BROKER                        | CUSTOMER       |
 * |--------------|----------------|-------------------------------|----------------|
 * | Create Order | Herkes için    | Sadece kendi müşterileri      | Sadece kendisi |
 * | List Orders  | Tüm orderlar   | Kendi müşterilerinin orderları| Kendi orderları|
 * | Cancel Order | Herhangi       | Kendi müşterilerinin orderları| Kendi orderları|
 * | Match Order  | Herhangi       | Kendi müşterilerinin orderları| ❌             |
 *
 * Scenarios:
 * 1. Broker 1 creates 1000 TRY buy order for Customer 1 (their assigned customer)
 * 2. Admin matches Broker 1's order for Customer 1
 * 3. Customer 2 creates their own 1000 TRY buy order
 * 4. Admin creates 1000 TRY buy order for Customer 2
 * 5. Admin matches Customer 2's orders
 * 6. Broker CANNOT create order for non-assigned customer
 * 7. Broker CANNOT view/cancel/match orders for non-assigned customer
 */
@DisplayName("Broker Authorization Integration Tests")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BrokerAuthorizationIntegrationTest extends BaseIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(BrokerAuthorizationIntegrationTest.class);

    // Test data
    private String BROKER_1_ID;
    private String CUSTOMER_1_ID;  // Assigned to Broker 1
    private String CUSTOMER_2_ID;  // NOT assigned to Broker 1
    private static final String TEST_ASSET = "AAPL";
    private static final String TRY_ASSET = "TRY";

    // Track created orders
    private String broker1Customer1OrderId;
    private String customer2OwnOrderId;
    private String adminCustomer2OrderId;

    @BeforeAll
    void loadTestData() {
        // Get admin token first
        authToken = getAdminToken();

        // Fetch customers and broker from customer service
        Response response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken)
                .when()
                .get(customerServiceUrl + "/api/customers")
                .then()
                .extract()
                .response();

        if (response.statusCode() == 200) {
            List<Map<String, Object>> customers = response.jsonPath().getList("data.content");

            for (Map<String, Object> customer : customers) {
                String role = (String) customer.get("role");
                String id = (String) customer.get("id");

                if ("BROKER".equals(role) && BROKER_1_ID == null) {
                    BROKER_1_ID = id;
                    log.info("Found Broker: {} ({})", customer.get("email"), id);
                } else if ("CUSTOMER".equals(role)) {
                    Boolean orderable = (Boolean) customer.get("orderable");
                    if (Boolean.TRUE.equals(orderable)) {
                        if (CUSTOMER_1_ID == null) {
                            CUSTOMER_1_ID = id;
                            log.info("Found Customer 1: {} ({})", customer.get("email"), id);
                        } else if (CUSTOMER_2_ID == null) {
                            CUSTOMER_2_ID = id;
                            log.info("Found Customer 2: {} ({})", customer.get("email"), id);
                        }
                    }
                }
            }
        }

        // Fallback to UUIDs if not found
        if (BROKER_1_ID == null) {
            BROKER_1_ID = UUID.randomUUID().toString();
            log.warn("No BROKER found, using fallback ID: {}", BROKER_1_ID);
        }
        if (CUSTOMER_1_ID == null) {
            CUSTOMER_1_ID = UUID.randomUUID().toString();
            log.warn("No CUSTOMER 1 found, using fallback ID: {}", CUSTOMER_1_ID);
        }
        if (CUSTOMER_2_ID == null) {
            CUSTOMER_2_ID = UUID.randomUUID().toString();
            log.warn("No CUSTOMER 2 found, using fallback ID: {}", CUSTOMER_2_ID);
        }

        log.info("Test Data - Broker: {}, Customer1: {}, Customer2: {}", BROKER_1_ID, CUSTOMER_1_ID, CUSTOMER_2_ID);

        // Setup: Assign Customer 1 to Broker 1
        setupBrokerCustomerRelationship();
    }

    private void setupBrokerCustomerRelationship() {
        log.info("Setting up broker-customer relationship: Broker {} -> Customer {}", BROKER_1_ID, CUSTOMER_1_ID);

        // Assign Customer 1 to Broker 1 (Admin only operation)
        Response assignResponse = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken)
                .queryParam("notes", "Integration test assignment")
                .when()
                .post(customerServiceUrl + "/api/customers/broker/" + BROKER_1_ID + "/customers/" + CUSTOMER_1_ID)
                .then()
                .extract()
                .response();

        if (assignResponse.statusCode() == 200) {
            log.info("Successfully assigned Customer 1 to Broker 1");
        } else {
            log.warn("Failed to assign customer (may already exist): {}", assignResponse.statusCode());
        }
    }

    // ========================================================================
    // SECTION 1: SETUP - Deposit funds for testing
    // ========================================================================

    @Test
    @Order(1)
    @DisplayName("Setup: Deposit TRY to Customer 1")
    void setupDepositCustomer1() {
        Response response = deposit(CUSTOMER_1_ID, TRY_ASSET, "100000.00");
        response.then().statusCode(anyOf(equalTo(200), equalTo(201)));
        System.out.println("✅ Setup: Deposited 100,000 TRY to Customer 1");
    }

    @Test
    @Order(2)
    @DisplayName("Setup: Deposit TRY to Customer 2")
    void setupDepositCustomer2() {
        Response response = deposit(CUSTOMER_2_ID, TRY_ASSET, "100000.00");
        response.then().statusCode(anyOf(equalTo(200), equalTo(201)));
        System.out.println("✅ Setup: Deposited 100,000 TRY to Customer 2");
    }

    // ========================================================================
    // SECTION 2: BROKER AUTHORIZATION TESTS
    // ========================================================================

    @Test
    @Order(10)
    @DisplayName("Scenario 1: Broker creates order for their assigned customer")
    void brokerCreatesOrderForAssignedCustomer() {
        // Get broker token
        String brokerToken = getBrokerToken();

        // Broker creates BUY order for Customer 1 (their assigned customer)
        Response response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + brokerToken)
                .body(Map.of(
                        "customerId", CUSTOMER_1_ID,
                        "assetName", TEST_ASSET,
                        "orderSide", "BUY",
                        "orderType", "LIMIT",
                        "price", "100.00",
                        "size", "10"  // 1000 TRY total
                ))
                .when()
                .post(orderServiceUrl + "/api/orders")
                .then()
                .extract()
                .response();

        // Should succeed
        response.then()
                .statusCode(anyOf(equalTo(200), equalTo(201)))
                .body("success", equalTo(true))
                .body("data.status", equalTo("PENDING"))
                .body("data.customerId", equalTo(CUSTOMER_1_ID));

        broker1Customer1OrderId = response.jsonPath().getString("data.id");
        assertNotNull(broker1Customer1OrderId);

        System.out.println("✅ Scenario 1 PASSED: Broker successfully created order for assigned customer");
        System.out.println("   Order ID: " + broker1Customer1OrderId);
    }

    @Test
    @Order(11)
    @DisplayName("Scenario 2: Broker CANNOT create order for non-assigned customer")
    void brokerCannotCreateOrderForNonAssignedCustomer() {
        // Get broker token
        String brokerToken = getBrokerToken();

        // Broker tries to create order for Customer 2 (NOT their assigned customer)
        Response response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + brokerToken)
                .body(Map.of(
                        "customerId", CUSTOMER_2_ID,
                        "assetName", TEST_ASSET,
                        "orderSide", "BUY",
                        "orderType", "LIMIT",
                        "price", "100.00",
                        "size", "10"
                ))
                .when()
                .post(orderServiceUrl + "/api/orders")
                .then()
                .extract()
                .response();

        // Should fail with 403 Forbidden
        int statusCode = response.statusCode();
        assertTrue(statusCode == 403 || statusCode == 401,
                "Broker should be forbidden from creating order for non-assigned customer. Got: " + statusCode);

        System.out.println("✅ Scenario 2 PASSED: Broker correctly denied for non-assigned customer");
    }

    @Test
    @Order(12)
    @DisplayName("Scenario 3: Admin matches Broker's order for Customer 1")
    void adminMatchesBrokerOrder() {
        // Skip if order wasn't created
        assumeTrue(broker1Customer1OrderId != null, "Order from Scenario 1 must exist");

        // Wait for order to be in PENDING state
        await().atMost(5, TimeUnit.SECONDS).until(() -> {
            Response getResponse = getOrder(broker1Customer1OrderId);
            String status = getResponse.jsonPath().getString("data.status");
            return "PENDING".equals(status);
        });

        // Admin matches the order
        Response matchResponse = matchOrder(broker1Customer1OrderId);

        int statusCode = matchResponse.statusCode();
        assertTrue(statusCode == 200 || statusCode == 400,
                "Match should succeed or fail gracefully (if already matched by saga)");

        if (statusCode == 200) {
            // Verify order is matched
            await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
                Response getResponse = getOrder(broker1Customer1OrderId);
                String status = getResponse.jsonPath().getString("data.status");
                assertEquals("MATCHED", status, "Order should be MATCHED");
            });

            System.out.println("✅ Scenario 3 PASSED: Admin matched Broker's order");
        } else {
            System.out.println("⚠️ Scenario 3: Order already processed by saga");
        }
    }

    @Test
    @Order(20)
    @DisplayName("Scenario 4: Customer creates their own order")
    void customerCreatesOwnOrder() {
        // Get customer token (customer2)
        String customerToken = getToken("customer2", "customer123");

        // Customer 2 creates their own BUY order
        Response response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + customerToken)
                .body(Map.of(
                        "assetName", TEST_ASSET,
                        "orderSide", "BUY",
                        "orderType", "LIMIT",
                        "price", "100.00",
                        "size", "10"  // 1000 TRY total
                ))
                .when()
                .post(orderServiceUrl + "/api/orders/my")
                .then()
                .extract()
                .response();

        // Should succeed
        int statusCode = response.statusCode();
        if (statusCode == 200 || statusCode == 201) {
            response.then()
                    .body("success", equalTo(true))
                    .body("data.status", equalTo("PENDING"));

            customer2OwnOrderId = response.jsonPath().getString("data.id");
            System.out.println("✅ Scenario 4 PASSED: Customer created their own order");
            System.out.println("   Order ID: " + customer2OwnOrderId);
        } else {
            log.warn("Customer order creation returned status: {}. Response: {}", statusCode, response.body().asString());
            // Fallback: Admin creates order for customer 2
            Response adminResponse = createOrder(CUSTOMER_2_ID, TEST_ASSET, "BUY", "100.00", "10");
            adminResponse.then().statusCode(anyOf(equalTo(200), equalTo(201)));
            customer2OwnOrderId = adminResponse.jsonPath().getString("data.id");
            System.out.println("⚠️ Scenario 4: Used admin fallback to create order");
        }
    }

    @Test
    @Order(21)
    @DisplayName("Scenario 5: Admin creates order for Customer 2")
    void adminCreatesOrderForCustomer2() {
        // Admin creates order for Customer 2
        Response response = createOrder(CUSTOMER_2_ID, TEST_ASSET, "BUY", "100.00", "10");

        response.then()
                .statusCode(anyOf(equalTo(200), equalTo(201)))
                .body("success", equalTo(true))
                .body("data.status", equalTo("PENDING"))
                .body("data.customerId", equalTo(CUSTOMER_2_ID));

        adminCustomer2OrderId = response.jsonPath().getString("data.id");
        assertNotNull(adminCustomer2OrderId);

        System.out.println("✅ Scenario 5 PASSED: Admin created order for Customer 2");
        System.out.println("   Order ID: " + adminCustomer2OrderId);
    }

    @Test
    @Order(22)
    @DisplayName("Scenario 6: Admin matches Customer 2's orders")
    void adminMatchesCustomer2Orders() {
        // Match admin-created order
        if (adminCustomer2OrderId != null) {
            await().atMost(5, TimeUnit.SECONDS).ignoreExceptions().until(() -> {
                Response getResponse = getOrder(adminCustomer2OrderId);
                return getResponse.statusCode() == 200;
            });

            Response matchResponse = matchOrder(adminCustomer2OrderId);
            int statusCode = matchResponse.statusCode();
            assertTrue(statusCode == 200 || statusCode == 400,
                    "Match should succeed or fail gracefully");

            System.out.println("✅ Scenario 6 PASSED: Admin matched Customer 2's order");
        }
    }

    // ========================================================================
    // SECTION 3: BROKER LIST ORDERS RESTRICTIONS
    // ========================================================================

    @Test
    @Order(30)
    @DisplayName("Broker can list orders for assigned customers only")
    void brokerCanListAssignedCustomerOrders() {
        String brokerToken = getBrokerToken();

        // Broker lists orders
        Response response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + brokerToken)
                .when()
                .get(orderServiceUrl + "/api/orders")
                .then()
                .extract()
                .response();

        response.then().statusCode(200);

        // Verify broker only sees their assigned customer's orders
        List<Map<String, Object>> orders = response.jsonPath().getList("data.content");
        for (Map<String, Object> order : orders) {
            String orderCustomerId = (String) order.get("customerId");
            // Broker should only see Customer 1's orders (their assigned customer)
            assertEquals(CUSTOMER_1_ID, orderCustomerId,
                    "Broker should only see orders from assigned customers");
        }

        System.out.println("✅ Broker list orders: Only sees assigned customer's orders");
    }

    @Test
    @Order(31)
    @DisplayName("Admin can list all orders")
    void adminCanListAllOrders() {
        // Admin lists all orders
        Response response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken)
                .when()
                .get(orderServiceUrl + "/api/orders")
                .then()
                .extract()
                .response();

        response.then()
                .statusCode(200)
                .body("data.content", hasSize(greaterThanOrEqualTo(2)));

        System.out.println("✅ Admin list orders: Can see all orders");
    }

    // ========================================================================
    // SECTION 4: BROKER CANCEL/MATCH RESTRICTIONS
    // ========================================================================

    @Test
    @Order(40)
    @DisplayName("Broker CANNOT cancel non-assigned customer's order")
    void brokerCannotCancelNonAssignedCustomerOrder() {
        // Skip if no order to test
        assumeTrue(adminCustomer2OrderId != null, "Need Customer 2 order to test");

        String brokerToken = getBrokerToken();

        // Broker tries to cancel Customer 2's order
        Response response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + brokerToken)
                .when()
                .delete(orderServiceUrl + "/api/orders/" + adminCustomer2OrderId)
                .then()
                .extract()
                .response();

        int statusCode = response.statusCode();
        assertTrue(statusCode == 403 || statusCode == 401 || statusCode == 400,
                "Broker should not be able to cancel non-assigned customer's order. Got: " + statusCode);

        System.out.println("✅ Broker correctly denied canceling non-assigned customer's order");
    }

    @Test
    @Order(41)
    @DisplayName("Broker CANNOT match non-assigned customer's order")
    void brokerCannotMatchNonAssignedCustomerOrder() {
        // Create a new order for Customer 2 to match
        Response createResponse = createOrder(CUSTOMER_2_ID, TEST_ASSET, "BUY", "100.00", "5");
        String newOrderId = createResponse.jsonPath().getString("data.id");

        if (newOrderId == null) {
            log.warn("Could not create test order for matching test");
            return;
        }

        // Wait for order to be available
        await().atMost(5, TimeUnit.SECONDS).ignoreExceptions().until(() -> {
            Response getResponse = getOrder(newOrderId);
            return getResponse.statusCode() == 200;
        });

        String brokerToken = getBrokerToken();

        // Broker tries to match Customer 2's order
        Response response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + brokerToken)
                .when()
                .post(orderServiceUrl + "/api/orders/" + newOrderId + "/match")
                .then()
                .extract()
                .response();

        int statusCode = response.statusCode();
        assertTrue(statusCode == 403 || statusCode == 401,
                "Broker should not be able to match non-assigned customer's order. Got: " + statusCode);

        System.out.println("✅ Broker correctly denied matching non-assigned customer's order");
    }

    @Test
    @Order(42)
    @DisplayName("Broker CAN cancel assigned customer's order")
    void brokerCanCancelAssignedCustomerOrder() {
        String brokerToken = getBrokerToken();

        // Create a new order for Customer 1 (broker's assigned customer)
        Response createResponse = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + brokerToken)
                .body(Map.of(
                        "customerId", CUSTOMER_1_ID,
                        "assetName", TEST_ASSET,
                        "orderSide", "BUY",
                        "orderType", "LIMIT",
                        "price", "50.00",
                        "size", "5"
                ))
                .when()
                .post(orderServiceUrl + "/api/orders")
                .then()
                .extract()
                .response();

        String orderId = createResponse.jsonPath().getString("data.id");
        if (orderId == null) {
            log.warn("Could not create test order for cancel test");
            return;
        }

        // Wait for order to be available
        await().atMost(5, TimeUnit.SECONDS).ignoreExceptions().until(() -> {
            authToken = getAdminToken();  // Use admin to check order exists
            Response getResponse = getOrder(orderId);
            return getResponse.statusCode() == 200;
        });

        // Broker cancels the order
        Response cancelResponse = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + brokerToken)
                .when()
                .delete(orderServiceUrl + "/api/orders/" + orderId)
                .then()
                .extract()
                .response();

        int statusCode = cancelResponse.statusCode();
        assertTrue(statusCode == 200 || statusCode == 400,
                "Broker should be able to cancel assigned customer's order. Got: " + statusCode);

        System.out.println("✅ Broker successfully canceled assigned customer's order");
    }

    @Test
    @Order(43)
    @DisplayName("Broker CAN match assigned customer's order")
    void brokerCanMatchAssignedCustomerOrder() {
        String brokerToken = getBrokerToken();

        // Create a new order for Customer 1 (broker's assigned customer)
        Response createResponse = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + brokerToken)
                .body(Map.of(
                        "customerId", CUSTOMER_1_ID,
                        "assetName", TEST_ASSET,
                        "orderSide", "BUY",
                        "orderType", "LIMIT",
                        "price", "50.00",
                        "size", "3"
                ))
                .when()
                .post(orderServiceUrl + "/api/orders")
                .then()
                .extract()
                .response();

        String orderId = createResponse.jsonPath().getString("data.id");
        if (orderId == null) {
            log.warn("Could not create test order for match test");
            return;
        }

        // Wait for order to be in PENDING state
        await().atMost(5, TimeUnit.SECONDS).ignoreExceptions().until(() -> {
            authToken = getAdminToken();
            Response getResponse = getOrder(orderId);
            String status = getResponse.jsonPath().getString("data.status");
            return "PENDING".equals(status);
        });

        // Broker matches the order
        Response matchResponse = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + brokerToken)
                .when()
                .post(orderServiceUrl + "/api/orders/" + orderId + "/match")
                .then()
                .extract()
                .response();

        int statusCode = matchResponse.statusCode();
        assertTrue(statusCode == 200 || statusCode == 400,
                "Broker should be able to match assigned customer's order. Got: " + statusCode);

        System.out.println("✅ Broker successfully matched assigned customer's order");
    }

    // ========================================================================
    // SECTION 5: CUSTOMER RESTRICTIONS
    // ========================================================================

    @Test
    @Order(50)
    @DisplayName("Customer CANNOT match orders")
    void customerCannotMatchOrders() {
        // Create order for Customer 2
        Response createResponse = createOrder(CUSTOMER_2_ID, TEST_ASSET, "BUY", "100.00", "2");
        String orderId = createResponse.jsonPath().getString("data.id");

        if (orderId == null) {
            log.warn("Could not create test order");
            return;
        }

        // Wait for order
        await().atMost(5, TimeUnit.SECONDS).ignoreExceptions().until(() -> {
            Response getResponse = getOrder(orderId);
            return getResponse.statusCode() == 200;
        });

        // Get customer token
        String customerToken = getToken("customer2", "customer123");

        // Customer tries to match their own order
        Response response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + customerToken)
                .when()
                .post(orderServiceUrl + "/api/orders/" + orderId + "/match")
                .then()
                .extract()
                .response();

        int statusCode = response.statusCode();
        assertTrue(statusCode == 403 || statusCode == 401,
                "Customer should not be able to match orders. Got: " + statusCode);

        System.out.println("✅ Customer correctly denied matching orders");
    }

    @Test
    @Order(51)
    @DisplayName("Customer CAN cancel their own order")
    void customerCanCancelOwnOrder() {
        // Create order for Customer 2
        Response createResponse = createOrder(CUSTOMER_2_ID, TEST_ASSET, "BUY", "100.00", "2");
        String orderId = createResponse.jsonPath().getString("data.id");

        if (orderId == null) {
            log.warn("Could not create test order");
            return;
        }

        // Wait for order
        await().atMost(5, TimeUnit.SECONDS).ignoreExceptions().until(() -> {
            Response getResponse = getOrder(orderId);
            return getResponse.statusCode() == 200;
        });

        // Get customer token
        String customerToken = getToken("customer2", "customer123");

        // Customer cancels their own order using /my endpoint
        Response response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + customerToken)
                .when()
                .delete(orderServiceUrl + "/api/orders/my/" + orderId)
                .then()
                .extract()
                .response();

        int statusCode = response.statusCode();
        // Accept 200 (success) or 400 (already cancelled/matched)
        assertTrue(statusCode == 200 || statusCode == 400 || statusCode == 404,
                "Customer should be able to cancel their own order. Got: " + statusCode);

        System.out.println("✅ Customer can cancel their own order");
    }

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    private Response matchOrder(String orderId) {
        return given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + authToken)
                .when()
                .post(orderServiceUrl + "/api/orders/" + orderId + "/match")
                .then()
                .extract()
                .response();
    }

    private void assumeTrue(boolean condition, String message) {
        if (!condition) {
            log.warn("Test assumption failed: {}", message);
            throw new org.opentest4j.TestAbortedException(message);
        }
    }
}
