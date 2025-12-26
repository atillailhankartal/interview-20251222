package com.brokage.customer.controller;

import com.brokage.common.dto.ApiResponse;
import com.brokage.customer.dto.CustomerDTO;
import com.brokage.customer.service.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Internal API endpoints for service-to-service communication.
 * These endpoints are not protected by JWT authentication.
 * Only accessible from internal network (configured in SecurityConfig).
 */
@RestController
@RequestMapping("/internal/customers")
@RequiredArgsConstructor
@Slf4j
public class InternalController {

    private final CustomerService customerService;

    /**
     * Get customer by email - used by Asset Service to resolve customer ID
     */
    @GetMapping("/by-email")
    public ResponseEntity<ApiResponse<CustomerDTO>> getCustomerByEmail(@RequestParam String email) {
        log.debug("Internal: Getting customer by email: {}", email);
        CustomerDTO customer = customerService.getCustomerByEmail(email);
        return ResponseEntity.ok(ApiResponse.success(customer));
    }

    /**
     * Get customer by ID - used by other services
     */
    @GetMapping("/{customerId}")
    public ResponseEntity<ApiResponse<CustomerDTO>> getCustomerById(@PathVariable java.util.UUID customerId) {
        log.debug("Internal: Getting customer by ID: {}", customerId);
        CustomerDTO customer = customerService.getCustomerById(customerId);
        return ResponseEntity.ok(ApiResponse.success(customer));
    }

    /**
     * Check if customer exists
     */
    @GetMapping("/{customerId}/exists")
    public ResponseEntity<ApiResponse<Boolean>> customerExists(@PathVariable java.util.UUID customerId) {
        log.debug("Internal: Checking if customer exists: {}", customerId);
        try {
            customerService.getCustomerById(customerId);
            return ResponseEntity.ok(ApiResponse.success(true));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.success(false));
        }
    }
}
