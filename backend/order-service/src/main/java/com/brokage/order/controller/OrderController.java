package com.brokage.order.controller;

import com.brokage.common.dto.ApiResponse;
import com.brokage.common.dto.PageResponse;
import com.brokage.common.enums.OrderSide;
import com.brokage.common.enums.OrderStatus;
import com.brokage.order.dto.CreateOrderRequest;
import com.brokage.order.dto.OrderDTO;
import com.brokage.order.dto.OrderFilterRequest;
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

    /**
     * Create a new order
     * - ADMIN: Can create orders for any customer
     * - CUSTOMER: Can only create orders for themselves
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'BROKER', 'CUSTOMER')")
    public ResponseEntity<ApiResponse<OrderDTO>> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        // For CUSTOMER role, enforce creating order for self only
        if (isCustomer(jwt) && !isAdmin(jwt)) {
            UUID tokenCustomerId = getCustomerId(jwt);
            if (!tokenCustomerId.equals(request.getCustomerId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("You can only create orders for yourself"));
            }
        }

        OrderDTO order = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(order, "Order created successfully"));
    }

    /**
     * Get order by ID
     * - ADMIN/BROKER: Can view any order
     * - CUSTOMER: Can only view their own orders
     */
    @GetMapping("/{orderId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'BROKER', 'CUSTOMER')")
    public ResponseEntity<ApiResponse<OrderDTO>> getOrder(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal Jwt jwt) {

        OrderDTO order;
        if (isAdmin(jwt) || isBroker(jwt)) {
            order = orderService.getOrderById(orderId);
        } else {
            UUID customerId = getCustomerId(jwt);
            order = orderService.getOrderByIdForCustomer(orderId, customerId);
        }

        return ResponseEntity.ok(ApiResponse.success(order));
    }

    /**
     * List orders with filters
     * - ADMIN/BROKER: Can list all orders (with optional customerId filter)
     * - CUSTOMER: Can only list their own orders
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'BROKER', 'CUSTOMER')")
    public ResponseEntity<ApiResponse<PageResponse<OrderDTO>>> listOrders(
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) String assetSymbol,
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
                .assetSymbol(assetSymbol)
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
        if (isAdmin(jwt) || isBroker(jwt)) {
            orders = orderService.listOrders(filter);
        } else {
            UUID tokenCustomerId = getCustomerId(jwt);
            orders = orderService.listOrdersForCustomer(tokenCustomerId, filter);
        }

        PageResponse<OrderDTO> pageResponse = PageResponse.of(orders);
        return ResponseEntity.ok(ApiResponse.success(pageResponse));
    }

    /**
     * Cancel an order
     * - ADMIN/BROKER: Can cancel any PENDING order
     * - CUSTOMER: Can only cancel their own PENDING orders
     */
    @DeleteMapping("/{orderId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'BROKER', 'CUSTOMER')")
    public ResponseEntity<ApiResponse<OrderDTO>> cancelOrder(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal Jwt jwt) {

        OrderDTO order;
        if (isAdmin(jwt) || isBroker(jwt)) {
            order = orderService.cancelOrder(orderId);
        } else {
            UUID customerId = getCustomerId(jwt);
            order = orderService.cancelOrderForCustomer(orderId, customerId);
        }

        return ResponseEntity.ok(ApiResponse.success(order, "Order cancelled successfully"));
    }

    /**
     * Match an order (ADMIN only)
     * Simulates order execution by matching buy/sell orders
     */
    @PostMapping("/{orderId}/match")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OrderDTO>> matchOrder(@PathVariable UUID orderId) {
        OrderDTO order = orderService.matchOrder(orderId);
        return ResponseEntity.ok(ApiResponse.success(order, "Order matched successfully"));
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
            return UUID.fromString(customerIdClaim);
        }

        // Fall back to subject (user ID in Keycloak)
        return UUID.fromString(jwt.getSubject());
    }
}
