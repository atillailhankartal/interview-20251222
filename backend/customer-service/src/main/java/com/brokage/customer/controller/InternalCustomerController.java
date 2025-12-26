package com.brokage.customer.controller;

import com.brokage.common.dto.ApiResponse;
import com.brokage.customer.dto.CustomerDTO;
import com.brokage.customer.service.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Internal API endpoints for service-to-service communication.
 * These endpoints do not require authentication as they are only accessible
 * within the Docker network and not exposed externally.
 */
@RestController
@RequestMapping("/internal/customers")
@RequiredArgsConstructor
@Slf4j
public class InternalCustomerController {

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
     * Get customer by ID - used by order-service to check if customer is orderable
     */
    @GetMapping("/{customerId}")
    public ResponseEntity<CustomerDTO> getCustomerInternal(@PathVariable UUID customerId) {
        log.debug("Internal request: Getting customer by ID: {}", customerId);
        try {
            CustomerDTO customer = customerService.getCustomerById(customerId);
            return ResponseEntity.ok(customer);
        } catch (Exception e) {
            log.warn("Customer not found: {}", customerId);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Check if customer exists
     */
    @GetMapping("/{customerId}/exists")
    public ResponseEntity<ApiResponse<Boolean>> customerExists(@PathVariable UUID customerId) {
        log.debug("Internal: Checking if customer exists: {}", customerId);
        try {
            customerService.getCustomerById(customerId);
            return ResponseEntity.ok(ApiResponse.success(true));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.success(false));
        }
    }

    /**
     * Check if customer is orderable (has CUSTOMER role)
     */
    @GetMapping("/{customerId}/orderable")
    public ResponseEntity<ApiResponse<Boolean>> isCustomerOrderable(@PathVariable UUID customerId) {
        log.debug("Internal request: Checking if customer {} is orderable", customerId);
        try {
            CustomerDTO customer = customerService.getCustomerById(customerId);
            boolean orderable = customer.isOrderable();
            return ResponseEntity.ok(ApiResponse.success(orderable));
        } catch (Exception e) {
            log.warn("Customer not found for orderable check: {}", customerId);
            return ResponseEntity.ok(ApiResponse.success(false));
        }
    }
}
