package com.brokage.customer.controller;

import com.brokage.common.dto.ApiResponse;
import com.brokage.common.dto.PageResponse;
import com.brokage.common.enums.CustomerRole;
import com.brokage.common.enums.CustomerStatus;
import com.brokage.common.enums.CustomerTier;
import com.brokage.customer.dto.BrokerCustomerDTO;
import com.brokage.customer.dto.CreateCustomerRequest;
import com.brokage.customer.dto.CustomerDTO;
import com.brokage.customer.dto.CustomerFilterRequest;
import com.brokage.customer.dto.CustomerStatsDTO;
import com.brokage.customer.dto.UpdateCustomerRequest;
import com.brokage.customer.service.BrokerCustomerService;
import com.brokage.customer.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@Slf4j
public class CustomerController {

    private final CustomerService customerService;
    private final BrokerCustomerService brokerCustomerService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CustomerDTO>> createCustomer(
            @Valid @RequestBody CreateCustomerRequest request) {
        log.info("Creating customer: {}", request.getEmail());
        CustomerDTO customer = customerService.createCustomer(request);
        return ResponseEntity.ok(ApiResponse.success(customer, "Customer created successfully"));
    }

    @GetMapping("/{customerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'BROKER') or @customerSecurityService.isOwnCustomer(#customerId, authentication)")
    public ResponseEntity<ApiResponse<CustomerDTO>> getCustomer(@PathVariable UUID customerId) {
        CustomerDTO customer = customerService.getCustomerById(customerId);
        return ResponseEntity.ok(ApiResponse.success(customer));
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'BROKER')")
    public ResponseEntity<ApiResponse<CustomerDTO>> getCurrentCustomer(@AuthenticationPrincipal Jwt jwt) {
        String keycloakUserId = jwt.getSubject();
        String email = jwt.getClaimAsString("email");
        // Use getOrLinkCustomerByKeycloakUserId to auto-link seeded users on first access
        CustomerDTO customer = customerService.getOrLinkCustomerByKeycloakUserId(keycloakUserId, email);
        return ResponseEntity.ok(ApiResponse.success(customer));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'BROKER')")
    public ResponseEntity<ApiResponse<PageResponse<CustomerDTO>>> listCustomers(
            @RequestParam(required = false) CustomerTier tier,
            @RequestParam(required = false) CustomerStatus status,
            @RequestParam(required = false) CustomerRole role,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal Jwt jwt) {

        // Check if user is BROKER - if so, only show their assigned customers
        if (isBrokerRole(jwt)) {
            // Get broker's customer ID by looking up the customer by Keycloak user ID
            // Use getOrLinkCustomerByKeycloakUserId to auto-link seeded users on first login
            String keycloakUserId = jwt.getSubject();
            String email = jwt.getClaimAsString("email");
            try {
                CustomerDTO broker = customerService.getOrLinkCustomerByKeycloakUserId(keycloakUserId, email);
                log.debug("Broker {} fetching their assigned customers", broker.getId());
                PageResponse<CustomerDTO> customers = brokerCustomerService.getBrokerCustomers(
                        broker.getId(), page, size);
                return ResponseEntity.ok(ApiResponse.success(customers));
            } catch (Exception e) {
                log.warn("Could not find broker customer for Keycloak user {}: {}", keycloakUserId, e.getMessage());
                // Fall through to show all customers if broker lookup fails
            }
        }

        // ADMIN sees all customers
        CustomerFilterRequest filter = CustomerFilterRequest.builder()
                .tier(tier)
                .status(status)
                .role(role)
                .search(search)
                .page(page)
                .size(size)
                .build();

        PageResponse<CustomerDTO> customers = customerService.listCustomers(filter);
        return ResponseEntity.ok(ApiResponse.success(customers));
    }

    @PutMapping("/{customerId}")
    @PreAuthorize("hasRole('ADMIN') or @customerSecurityService.isOwnCustomer(#customerId, authentication)")
    public ResponseEntity<ApiResponse<CustomerDTO>> updateCustomer(
            @PathVariable UUID customerId,
            @Valid @RequestBody UpdateCustomerRequest request) {
        log.info("Updating customer: {}", customerId);
        CustomerDTO customer = customerService.updateCustomer(customerId, request);
        return ResponseEntity.ok(ApiResponse.success(customer, "Customer updated successfully"));
    }

    @PutMapping("/{customerId}/tier")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CustomerDTO>> updateCustomerTier(
            @PathVariable UUID customerId,
            @RequestParam CustomerTier tier) {
        log.info("Updating customer {} tier to {}", customerId, tier);
        CustomerDTO customer = customerService.updateCustomerTier(customerId, tier);
        return ResponseEntity.ok(ApiResponse.success(customer, "Customer tier updated successfully"));
    }

    @PutMapping("/{customerId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CustomerDTO>> updateCustomerStatus(
            @PathVariable UUID customerId,
            @RequestParam CustomerStatus status) {
        log.info("Updating customer {} status to {}", customerId, status);
        CustomerDTO customer = customerService.updateCustomerStatus(customerId, status);
        return ResponseEntity.ok(ApiResponse.success(customer, "Customer status updated successfully"));
    }

    @PostMapping("/{customerId}/link-keycloak")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CustomerDTO>> linkKeycloakUser(
            @PathVariable UUID customerId,
            @RequestParam String keycloakUserId) {
        log.info("Linking customer {} to Keycloak user {}", customerId, keycloakUserId);
        CustomerDTO customer = customerService.linkKeycloakUser(customerId, keycloakUserId);
        return ResponseEntity.ok(ApiResponse.success(customer, "Keycloak user linked successfully"));
    }

    @GetMapping("/by-email")
    @PreAuthorize("hasAnyRole('ADMIN', 'BROKER', 'CUSTOMER')")
    public ResponseEntity<ApiResponse<CustomerDTO>> getCustomerByEmail(@RequestParam String email) {
        CustomerDTO customer = customerService.getCustomerByEmail(email);
        return ResponseEntity.ok(ApiResponse.success(customer));
    }

    /**
     * Get customer statistics for dashboard (ADMIN only)
     */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CustomerStatsDTO>> getCustomerStats() {
        CustomerStatsDTO stats = customerService.getStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    /**
     * Get orderable customers for order creation (ADMIN and BROKER only)
     * Only returns customers with CUSTOMER role (not ADMIN or BROKER)
     * BROKER only sees their assigned customers
     * Supports search and pagination for remote select component
     */
    @GetMapping("/for-order")
    @PreAuthorize("hasAnyRole('ADMIN', 'BROKER')")
    public ResponseEntity<ApiResponse<PageResponse<CustomerDTO>>> getCustomersForOrder(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal Jwt jwt) {

        log.debug("Getting orderable customers for order creation, search: {}, page: {}, size: {}", search, page, size);

        // Check if user is BROKER - if so, only show their assigned customers
        if (isBrokerRole(jwt)) {
            // Get broker's customer ID by looking up the customer by Keycloak user ID
            // Use getOrLinkCustomerByKeycloakUserId to auto-link seeded users on first login
            String keycloakUserId = jwt.getSubject();
            String email = jwt.getClaimAsString("email");
            try {
                CustomerDTO broker = customerService.getOrLinkCustomerByKeycloakUserId(keycloakUserId, email);
                log.debug("Broker {} fetching their orderable customers", broker.getId());
                PageResponse<CustomerDTO> customers = brokerCustomerService.getBrokerCustomersForOrder(
                        broker.getId(), search, page, size);
                return ResponseEntity.ok(ApiResponse.success(customers));
            } catch (Exception e) {
                log.warn("Could not find broker customer for Keycloak user {}: {}", keycloakUserId, e.getMessage());
                // Fall through to show all customers if broker lookup fails
            }
        }

        // ADMIN sees all orderable customers
        CustomerFilterRequest filter = CustomerFilterRequest.builder()
                .search(search)
                .orderableOnly(true)
                .page(page)
                .size(size)
                .build();

        PageResponse<CustomerDTO> customers = customerService.listCustomers(filter);
        return ResponseEntity.ok(ApiResponse.success(customers));
    }

    // ==================== BROKER-CUSTOMER ENDPOINTS ====================

    /**
     * Check if a customer belongs to a broker
     * Used by order-service for authorization
     */
    @GetMapping("/broker/{brokerId}/is-customer/{customerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'BROKER')")
    public ResponseEntity<ApiResponse<Boolean>> isBrokerOfCustomer(
            @PathVariable UUID brokerId,
            @PathVariable UUID customerId) {
        boolean isBrokerOf = brokerCustomerService.isBrokerOfCustomer(brokerId, customerId);
        return ResponseEntity.ok(ApiResponse.success(isBrokerOf));
    }

    /**
     * Get customer IDs for a broker
     * Used by order-service to filter orders
     */
    @GetMapping("/broker/{brokerId}/customer-ids")
    @PreAuthorize("hasAnyRole('ADMIN', 'BROKER')")
    public ResponseEntity<ApiResponse<java.util.List<UUID>>> getBrokerCustomerIds(
            @PathVariable UUID brokerId) {
        java.util.List<UUID> customerIds = brokerCustomerService.getBrokerCustomerIds(brokerId);
        return ResponseEntity.ok(ApiResponse.success(customerIds));
    }

    /**
     * Get all customers for a broker (BROKER can only see their own, ADMIN can see all)
     */
    @GetMapping("/broker/{brokerId}/customers")
    @PreAuthorize("hasAnyRole('ADMIN', 'BROKER')")
    public ResponseEntity<ApiResponse<PageResponse<CustomerDTO>>> getBrokerCustomers(
            @PathVariable UUID brokerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.debug("Getting customers for broker: {}", brokerId);
        PageResponse<CustomerDTO> customers = brokerCustomerService.getBrokerCustomers(brokerId, page, size);
        return ResponseEntity.ok(ApiResponse.success(customers));
    }

    /**
     * Assign a customer to a broker (ADMIN only)
     */
    @PostMapping("/broker/{brokerId}/customers/{customerId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BrokerCustomerDTO>> assignCustomerToBroker(
            @PathVariable UUID brokerId,
            @PathVariable UUID customerId,
            @RequestParam(required = false) String notes) {
        log.info("Assigning customer {} to broker {}", customerId, brokerId);
        BrokerCustomerDTO result = brokerCustomerService.assignCustomerToBroker(brokerId, customerId, notes);
        return ResponseEntity.ok(ApiResponse.success(result, "Customer assigned to broker successfully"));
    }

    /**
     * Remove a customer from a broker (ADMIN only)
     */
    @DeleteMapping("/broker/{brokerId}/customers/{customerId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> removeCustomerFromBroker(
            @PathVariable UUID brokerId,
            @PathVariable UUID customerId) {
        log.info("Removing customer {} from broker {}", customerId, brokerId);
        brokerCustomerService.removeCustomerFromBroker(brokerId, customerId);
        return ResponseEntity.ok(ApiResponse.success(null, "Customer removed from broker successfully"));
    }

    /**
     * Get orderable customers for a specific broker (for order creation)
     * BROKER can only see their own customers
     */
    @GetMapping("/broker/{brokerId}/customers/for-order")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('BROKER') and #brokerId == authentication.principal.claims['customer_id'])")
    public ResponseEntity<ApiResponse<PageResponse<CustomerDTO>>> getBrokerCustomersForOrder(
            @PathVariable UUID brokerId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.debug("Getting orderable customers for broker: {}, search: {}", brokerId, search);
        PageResponse<CustomerDTO> customers = brokerCustomerService.getBrokerCustomers(brokerId, page, size);
        return ResponseEntity.ok(ApiResponse.success(customers));
    }

    // ==================== CUSTOMER-BROKER ENDPOINTS (Reverse lookup) ====================

    /**
     * Get all brokers assigned to a customer
     * ADMIN can see any customer's brokers, CUSTOMER can only see their own
     */
    @GetMapping("/customer/{customerId}/brokers")
    @PreAuthorize("hasRole('ADMIN') or @customerSecurityService.isOwnCustomer(#customerId, authentication)")
    public ResponseEntity<ApiResponse<PageResponse<CustomerDTO>>> getCustomerBrokers(
            @PathVariable UUID customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.debug("Getting brokers for customer: {}", customerId);
        PageResponse<CustomerDTO> brokers = brokerCustomerService.getCustomerBrokers(customerId, page, size);
        return ResponseEntity.ok(ApiResponse.success(brokers));
    }

    /**
     * Get broker IDs for a customer
     * Used by other services to check broker relationships
     */
    @GetMapping("/customer/{customerId}/broker-ids")
    @PreAuthorize("hasAnyRole('ADMIN', 'BROKER', 'CUSTOMER')")
    public ResponseEntity<ApiResponse<java.util.List<UUID>>> getCustomerBrokerIds(
            @PathVariable UUID customerId) {
        java.util.List<UUID> brokerIds = brokerCustomerService.getCustomerBrokerIds(customerId);
        return ResponseEntity.ok(ApiResponse.success(brokerIds));
    }

    // ==================== HELPER METHODS ====================

    @SuppressWarnings("unchecked")
    private boolean isBrokerRole(Jwt jwt) {
        Object realmAccess = jwt.getClaim("realm_access");
        if (realmAccess instanceof java.util.Map<?, ?> accessMap) {
            Object roles = accessMap.get("roles");
            if (roles instanceof java.util.List<?> roleList) {
                return roleList.stream().anyMatch(r -> "BROKER".equals(r));
            }
        }
        return false;
    }
}
