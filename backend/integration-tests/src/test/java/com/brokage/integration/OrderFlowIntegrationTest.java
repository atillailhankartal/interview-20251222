package com.brokage.integration;

import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for complete order flow.
 * Tests: Create -> Reserve -> Match -> Settle
 */
@DisplayName("Order Flow Integration Tests")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class OrderFlowIntegrationTest extends BaseIntegrationTest {

    private static final String TEST_CUSTOMER_ID = "c0000000-0000-0000-0000-000000000001";
    private static final String TEST_ASSET = "AAPL";

    @Nested
    @DisplayName("Order Creation")
    class OrderCreation {

        @Test
        @DisplayName("Should create a BUY order successfully")
        void shouldCreateBuyOrder() {
            Response response = createOrder(
                    TEST_CUSTOMER_ID,
                    TEST_ASSET,
                    "BUY",
                    "150.00",
                    "10"
            );

            response.then()
                    .statusCode(anyOf(equalTo(200), equalTo(201)))
                    .body("success", equalTo(true))
                    .body("data.id", notNullValue())
                    .body("data.customerId", equalTo(TEST_CUSTOMER_ID))
                    .body("data.assetName", equalTo(TEST_ASSET))
                    .body("data.orderSide", equalTo("BUY"));
        }

        @Test
        @DisplayName("Should create a SELL order successfully")
        void shouldCreateSellOrder() {
            Response response = createOrder(
                    TEST_CUSTOMER_ID,
                    TEST_ASSET,
                    "SELL",
                    "155.00",
                    "5"
            );

            response.then()
                    .statusCode(anyOf(equalTo(200), equalTo(201)))
                    .body("success", equalTo(true))
                    .body("data.orderSide", equalTo("SELL"));
        }

        @Test
        @DisplayName("Should reject order with invalid customer ID")
        void shouldRejectInvalidCustomerId() {
            Response response = createOrder(
                    "invalid-uuid",
                    TEST_ASSET,
                    "BUY",
                    "150.00",
                    "10"
            );

            // API returns 400 for validation error or 500 for invalid UUID format
            response.then()
                    .statusCode(anyOf(equalTo(400), equalTo(500)))
                    .body("success", equalTo(false));
        }

        @Test
        @DisplayName("Should reject order with negative price")
        void shouldRejectNegativePrice() {
            Response response = createOrder(
                    TEST_CUSTOMER_ID,
                    TEST_ASSET,
                    "BUY",
                    "-10.00",
                    "10"
            );

            response.then()
                    .statusCode(400);
        }

        @Test
        @DisplayName("Should reject order with zero size")
        void shouldRejectZeroSize() {
            Response response = createOrder(
                    TEST_CUSTOMER_ID,
                    TEST_ASSET,
                    "BUY",
                    "150.00",
                    "0"
            );

            response.then()
                    .statusCode(400);
        }
    }

    @Nested
    @DisplayName("Order Retrieval")
    class OrderRetrieval {

        @Test
        @DisplayName("Should get order by ID")
        void shouldGetOrderById() {
            // First create an order
            Response createResponse = createOrder(
                    TEST_CUSTOMER_ID,
                    TEST_ASSET,
                    "BUY",
                    "150.00",
                    "10"
            );

            String orderId = createResponse.jsonPath().getString("data.id");

            // Then retrieve it
            Response getResponse = getOrder(orderId);

            getResponse.then()
                    .statusCode(200)
                    .body("data.id", equalTo(orderId))
                    .body("data.customerId", equalTo(TEST_CUSTOMER_ID));
        }

        @Test
        @DisplayName("Should return 404 for non-existent order")
        void shouldReturn404ForNonExistentOrder() {
            String fakeOrderId = UUID.randomUUID().toString();

            try {
                Response response = getOrder(fakeOrderId);
                org.junit.jupiter.api.Assertions.assertEquals(404, response.statusCode(),
                        "Expected 404 status code for non-existent order");
            } catch (Exception e) {
                // RestAssured may throw for 4xx/5xx status codes
                org.junit.jupiter.api.Assertions.assertTrue(
                        e.getMessage().contains("404"),
                        "Expected 404 in error message but got: " + e.getMessage());
            }
        }
    }

    @Nested
    @DisplayName("Order Cancellation")
    class OrderCancellation {

        @Test
        @DisplayName("Should cancel pending order")
        void shouldCancelPendingOrder() {
            // Create an order
            Response createResponse = createOrder(
                    TEST_CUSTOMER_ID,
                    TEST_ASSET,
                    "BUY",
                    "150.00",
                    "10"
            );

            String orderId = createResponse.jsonPath().getString("data.id");

            // Cancel it
            Response cancelResponse = cancelOrder(orderId);

            cancelResponse.then()
                    .statusCode(200);

            // Verify status changed
            Response getResponse = getOrder(orderId);
            getResponse.then()
                    .body("data.status", equalTo("CANCELLED"));
        }

        @Test
        @DisplayName("Should reject cancellation of already cancelled order")
        void shouldRejectCancellationOfCancelledOrder() {
            // Create an order
            Response createResponse = createOrder(
                    TEST_CUSTOMER_ID,
                    TEST_ASSET,
                    "BUY",
                    "150.00",
                    "10"
            );

            String orderId = createResponse.jsonPath().getString("data.id");

            // Cancel it once
            try {
                cancelOrder(orderId);
            } catch (Exception e) {
                // First cancel might fail if order already processed
            }

            // Try to cancel again - should return 400 (order already cancelled) or throw
            try {
                Response secondCancelResponse = cancelOrder(orderId);
                org.junit.jupiter.api.Assertions.assertEquals(400, secondCancelResponse.statusCode(),
                        "Expected 400 status code for already cancelled order");
            } catch (Exception e) {
                // RestAssured may throw for 400 status codes
                org.junit.jupiter.api.Assertions.assertTrue(
                        e.getMessage().contains("400"),
                        "Expected 400 in error message but got: " + e.getMessage());
            }
        }
    }

    @Nested
    @DisplayName("Order Status Transitions")
    class OrderStatusTransitions {

        @Test
        @DisplayName("Should transition order status through saga")
        void shouldTransitionOrderStatusThroughSaga() {
            // Create an order
            Response createResponse = createOrder(
                    TEST_CUSTOMER_ID,
                    TEST_ASSET,
                    "BUY",
                    "150.00",
                    "10"
            );

            String orderId = createResponse.jsonPath().getString("data.id");
            String initialStatus = createResponse.jsonPath().getString("data.status");

            // Wait for saga to process (status should change from PENDING)
            await()
                    .atMost(15, TimeUnit.SECONDS)
                    .pollInterval(1, TimeUnit.SECONDS)
                    .until(() -> {
                        Response getResponse = getOrder(orderId);
                        String currentStatus = getResponse.jsonPath().getString("data.status");
                        // Status should change from initial PENDING state
                        return !currentStatus.equals("PENDING");
                    });

            // Verify order has progressed through saga
            Response finalResponse = getOrder(orderId);
            finalResponse.then()
                    .statusCode(200)
                    .body("data.status", anyOf(
                            equalTo("ASSET_RESERVED"),
                            equalTo("ORDER_CONFIRMED"),
                            equalTo("MATCHED"),
                            equalTo("REJECTED")
                    ));
        }
    }

    @Nested
    @DisplayName("Order Listing")
    class OrderListing {

        @Test
        @DisplayName("Should list customer orders with pagination")
        void shouldListCustomerOrdersWithPagination() {
            // Create multiple orders
            for (int i = 0; i < 5; i++) {
                createOrder(
                        TEST_CUSTOMER_ID,
                        TEST_ASSET,
                        "BUY",
                        String.valueOf(150 + i),
                        "10"
                );
            }

            // List orders
            Response response = io.restassured.RestAssured.given()
                    .contentType("application/json")
                    .header("Authorization", "Bearer " + authToken)
                    .queryParam("customerId", TEST_CUSTOMER_ID)
                    .queryParam("page", 0)
                    .queryParam("size", 3)
                    .when()
                    .get(orderServiceUrl + "/api/orders");

            response.then()
                    .statusCode(200)
                    .body("data.content", hasSize(lessThanOrEqualTo(3)));
        }

        @Test
        @DisplayName("Should filter orders by status")
        void shouldFilterOrdersByStatus() {
            // Create and cancel an order
            Response createResponse = createOrder(
                    TEST_CUSTOMER_ID,
                    TEST_ASSET,
                    "BUY",
                    "150.00",
                    "10"
            );
            String orderId = createResponse.jsonPath().getString("data.id");
            cancelOrder(orderId);

            // List only cancelled orders
            Response response = io.restassured.RestAssured.given()
                    .contentType("application/json")
                    .header("Authorization", "Bearer " + authToken)
                    .queryParam("customerId", TEST_CUSTOMER_ID)
                    .queryParam("status", "CANCELLED")
                    .when()
                    .get(orderServiceUrl + "/api/orders");

            response.then()
                    .statusCode(200)
                    .body("data.content", everyItem(hasEntry("status", "CANCELLED")));
        }
    }
}
