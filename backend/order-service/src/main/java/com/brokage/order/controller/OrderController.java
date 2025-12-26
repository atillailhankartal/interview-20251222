package com.brokage.order.controller;

import com.brokage.common.dto.ApiResponse;
import com.brokage.common.dto.PageResponse;
import com.brokage.common.enums.OrderSide;
import com.brokage.common.enums.OrderStatus;
import com.brokage.order.client.CustomerClient;
import com.brokage.order.dto.CreateOrderRequest;
import com.brokage.order.dto.OrderDTO;
import com.brokage.order.dto.OrderFilterRequest;
import com.brokage.order.dto.OrderStatsDTO;
import com.brokage.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;
    private final CustomerClient customerClient;

    /**
     * Create a new order
     * - ADMIN: Can create orders for any customer (with CUSTOMER role)
     * - BROKER: Can only create orders for their own customers
     * - CUSTOMER: Can only create orders for themselves
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'BROKER', 'CUSTOMER')")
    public ResponseEntity<ApiResponse<OrderDTO>> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        UUID requestedCustomerId = request.getCustomerId();

        // CUSTOMER role: Can only create orders for themselves
        if (isCustomer(jwt) && !isAdmin(jwt) && !isBroker(jwt)) {
            UUID tokenCustomerId = getCustomerId(jwt);
            if (!tokenCustomerId.equals(requestedCustomerId)) {
                log.warn("Customer {} tried to create order for customer {}", tokenCustomerId, requestedCustomerId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("You can only create orders for yourself"));
            }
        }

        // BROKER role: Can only create orders for their own customers
        if (isBroker(jwt) && !isAdmin(jwt)) {
            UUID brokerId = getCustomerId(jwt);
            if (!customerClient.isBrokerOfCustomer(brokerId, requestedCustomerId)) {
                log.warn("Broker {} tried to create order for non-assigned customer {}", brokerId, requestedCustomerId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("You can only create orders for your assigned customers"));
            }
        }

        // ADMIN: No restrictions (but target must be orderable - checked in service)

        OrderDTO order = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(order, "Order created successfully"));
    }

    /**
     * Get order by ID
     * - ADMIN: Can view any order
     * - BROKER: Can only view their customers' orders
     * - CUSTOMER: Can only view their own orders
     */
    @GetMapping("/{orderId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'BROKER', 'CUSTOMER')")
    public ResponseEntity<ApiResponse<OrderDTO>> getOrder(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal Jwt jwt) {

        OrderDTO order = orderService.getOrderById(orderId);

        // ADMIN: Can view any order
        if (isAdmin(jwt)) {
            return ResponseEntity.ok(ApiResponse.success(order));
        }

        // BROKER: Can only view their customers' orders
        if (isBroker(jwt)) {
            UUID brokerId = getCustomerId(jwt);
            if (!customerClient.isBrokerOfCustomer(brokerId, order.getCustomerId())) {
                log.warn("Broker {} tried to view order {} of non-assigned customer {}",
                        brokerId, orderId, order.getCustomerId());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("You can only view orders of your assigned customers"));
            }
            return ResponseEntity.ok(ApiResponse.success(order));
        }

        // CUSTOMER: Can only view their own orders
        UUID customerId = getCustomerId(jwt);
        if (!order.getCustomerId().equals(customerId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("You can only view your own orders"));
        }

        return ResponseEntity.ok(ApiResponse.success(order));
    }

    /**
     * List orders with filters
     * - ADMIN: Can list all orders
     * - BROKER: Can only list their customers' orders
     * - CUSTOMER: Can only list their own orders
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'BROKER', 'CUSTOMER')")
    public ResponseEntity<ApiResponse<PageResponse<OrderDTO>>> listOrders(
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) String assetName,
            @RequestParam(required = false) OrderSide orderSide,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection,
            @AuthenticationPrincipal Jwt jwt) {

        OrderFilterRequest filter = OrderFilterRequest.builder()
                .customerId(customerId)
                .assetName(assetName)
                .orderSide(orderSide)
                .status(status)
                .startDate(startDate)
                .endDate(endDate)
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();

        Page<OrderDTO> orders;

        // ADMIN: Can list all orders
        if (isAdmin(jwt)) {
            orders = orderService.listOrders(filter);
        }
        // BROKER: Can list orders for their customers only
        else if (isBroker(jwt)) {
            UUID brokerId = getCustomerId(jwt);
            // If customerId filter is provided, verify it belongs to the broker
            if (customerId != null) {
                if (!customerClient.isBrokerOfCustomer(brokerId, customerId)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(ApiResponse.error("You can only view orders of your assigned customers"));
                }
                orders = orderService.listOrdersForCustomer(customerId, filter);
            } else {
                // List orders for all broker's customers
                orders = orderService.listOrdersForBroker(brokerId, filter);
            }
        }
        // CUSTOMER: Can only list their own orders
        else {
            UUID tokenCustomerId = getCustomerId(jwt);
            orders = orderService.listOrdersForCustomer(tokenCustomerId, filter);
        }

        PageResponse<OrderDTO> pageResponse = PageResponse.of(orders);
        return ResponseEntity.ok(ApiResponse.success(pageResponse));
    }

    /**
     * Cancel an order
     * - ADMIN: Can cancel any PENDING order
     * - BROKER: Can cancel their customers' PENDING orders
     * - CUSTOMER: Can only cancel their own PENDING orders
     */
    @DeleteMapping("/{orderId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'BROKER', 'CUSTOMER')")
    public ResponseEntity<ApiResponse<OrderDTO>> cancelOrder(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal Jwt jwt) {

        // First get the order to check ownership
        OrderDTO existingOrder = orderService.getOrderById(orderId);

        // ADMIN: Can cancel any order
        if (isAdmin(jwt)) {
            OrderDTO order = orderService.cancelOrder(orderId);
            return ResponseEntity.ok(ApiResponse.success(order, "Order cancelled successfully"));
        }

        // BROKER: Can cancel their customers' orders
        if (isBroker(jwt)) {
            UUID brokerId = getCustomerId(jwt);
            if (!customerClient.isBrokerOfCustomer(brokerId, existingOrder.getCustomerId())) {
                log.warn("Broker {} tried to cancel order {} of non-assigned customer {}",
                        brokerId, orderId, existingOrder.getCustomerId());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("You can only cancel orders of your assigned customers"));
            }
            OrderDTO order = orderService.cancelOrder(orderId);
            return ResponseEntity.ok(ApiResponse.success(order, "Order cancelled successfully"));
        }

        // CUSTOMER: Can only cancel their own orders
        UUID customerId = getCustomerId(jwt);
        OrderDTO order = orderService.cancelOrderForCustomer(orderId, customerId);
        return ResponseEntity.ok(ApiResponse.success(order, "Order cancelled successfully"));
    }

    /**
     * Match an order (ADMIN and BROKER for their customers)
     * Simulates order execution by matching buy/sell orders
     * - ADMIN: Can match any order
     * - BROKER: Can match their customers' orders
     */
    @PostMapping("/{orderId}/match")
    @PreAuthorize("hasAnyRole('ADMIN', 'BROKER')")
    public ResponseEntity<ApiResponse<OrderDTO>> matchOrder(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal Jwt jwt) {

        // First get the order to check ownership (for BROKER)
        OrderDTO existingOrder = orderService.getOrderById(orderId);

        // BROKER: Can only match their customers' orders
        if (isBroker(jwt) && !isAdmin(jwt)) {
            UUID brokerId = getCustomerId(jwt);
            if (!customerClient.isBrokerOfCustomer(brokerId, existingOrder.getCustomerId())) {
                log.warn("Broker {} tried to match order {} of non-assigned customer {}",
                        brokerId, orderId, existingOrder.getCustomerId());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("You can only match orders of your assigned customers"));
            }
        }

        OrderDTO order = orderService.matchOrder(orderId);
        return ResponseEntity.ok(ApiResponse.success(order, "Order matched successfully"));
    }

    /**
     * Get order statistics for dashboard
     */
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'BROKER')")
    public ResponseEntity<ApiResponse<OrderStatsDTO>> getOrderStats() {
        OrderStatsDTO stats = orderService.getOrderStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    // Helper methods for JWT claims extraction
    private boolean isAdmin(Jwt jwt) {
        return hasRole(jwt, "ADMIN");
    }

    private boolean isBroker(Jwt jwt) {
        return hasRole(jwt, "BROKER");
    }

    private boolean isCustomer(Jwt jwt) {
        return hasRole(jwt, "CUSTOMER");
    }

    private boolean hasRole(Jwt jwt, String role) {
        var realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null) return false;

        var roles = realmAccess.get("roles");
        if (roles instanceof java.util.Collection<?> roleList) {
            return roleList.stream()
                    .anyMatch(r -> r.toString().equalsIgnoreCase(role));
        }
        return false;
    }

    private UUID getCustomerId(Jwt jwt) {
        // First try to get from custom claim
        String customerIdClaim = jwt.getClaimAsString("customer_id");
        if (customerIdClaim != null) {
            try {
                return UUID.fromString(customerIdClaim);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid customer_id claim: {}", customerIdClaim);
            }
        }

        // Try to lookup by email
        String email = jwt.getClaimAsString("email");
        if (email != null) {
            UUID customerId = customerClient.getCustomerIdByEmail(email);
            if (customerId != null) {
                log.debug("Resolved customer ID {} from email {}", customerId, email);
                return customerId;
            }
        }

        // Fall back error - cannot determine customer ID
        log.error("Cannot determine customer ID from JWT. Subject: {}, Email: {}", jwt.getSubject(), email);
        throw new IllegalStateException("Cannot determine customer identity from token");
    }
}
