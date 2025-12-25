package com.brokage.order.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@Slf4j
public class CustomerClient {

    private final RestTemplate restTemplate;
    private final String customerServiceUrl;

    public CustomerClient(
            RestTemplate restTemplate,
            @Value("${services.customer-service.url}") String customerServiceUrl) {
        this.restTemplate = restTemplate;
        this.customerServiceUrl = customerServiceUrl;
    }

    /**
     * Check if a customer can have orders created for them.
     * Returns true only for customers with CUSTOMER role.
     * Uses internal endpoint (no authentication required for service-to-service calls).
     */
    public boolean isCustomerOrderable(UUID customerId) {
        try {
            // Use internal endpoint for service-to-service communication
            String url = customerServiceUrl + "/internal/customers/" + customerId;
            log.debug("Checking if customer {} is orderable at {}", customerId, url);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response == null) {
                log.warn("Customer {} not found", customerId);
                return false;
            }

            Boolean orderable = (Boolean) response.get("orderable");
            log.debug("Customer {} orderable status: {}", customerId, orderable);

            return Boolean.TRUE.equals(orderable);
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("Customer {} not found", customerId);
            return false;
        } catch (Exception e) {
            log.error("Error checking customer {} orderable status: {}", customerId, e.getMessage());
            // Fail open or closed? For security, fail closed.
            return false;
        }
    }

    /**
     * Get customer role.
     * Uses internal endpoint (no authentication required for service-to-service calls).
     */
    public String getCustomerRole(UUID customerId) {
        try {
            // Use internal endpoint for service-to-service communication
            String url = customerServiceUrl + "/internal/customers/" + customerId;

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response == null) {
                return null;
            }

            return (String) response.get("role");
        } catch (Exception e) {
            log.error("Error getting customer {} role: {}", customerId, e.getMessage());
            return null;
        }
    }

    /**
     * Get all customer IDs for a broker.
     */
    @SuppressWarnings("unchecked")
    public List<UUID> getBrokerCustomerIds(UUID brokerId) {
        try {
            String url = customerServiceUrl + "/api/customers/broker/" + brokerId + "/customer-ids";
            log.debug("Getting customer IDs for broker {} at {}", brokerId, url);

            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response == null || response.get("data") == null) {
                return List.of();
            }

            List<String> customerIdStrings = (List<String>) response.get("data");
            return customerIdStrings.stream()
                    .map(UUID::fromString)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error getting broker {} customer IDs: {}", brokerId, e.getMessage());
            return List.of();
        }
    }

    /**
     * Get customer ID by email.
     * Used when customer_id claim is not in JWT.
     */
    public UUID getCustomerIdByEmail(String email) {
        try {
            String url = customerServiceUrl + "/api/customers/by-email?email=" + email;
            log.debug("Getting customer ID for email {} at {}", email, url);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response == null || response.get("data") == null) {
                log.warn("Customer not found for email: {}", email);
                return null;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) response.get("data");
            String customerId = (String) data.get("id");

            if (customerId != null) {
                UUID result = UUID.fromString(customerId);
                log.debug("Found customer ID {} for email {}", result, email);
                return result;
            }

            return null;
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("Customer not found for email: {}", email);
            return null;
        } catch (Exception e) {
            log.error("Error getting customer by email {}: {}", email, e.getMessage());
            return null;
        }
    }

    /**
     * Check if a customer belongs to a broker.
     * Returns true if the broker-customer relationship exists and is active.
     */
    public boolean isBrokerOfCustomer(UUID brokerId, UUID customerId) {
        try {
            String url = customerServiceUrl + "/api/customers/broker/" + brokerId + "/is-customer/" + customerId;
            log.debug("Checking if broker {} is broker of customer {} at {}", brokerId, customerId, url);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response == null) {
                return false;
            }

            Boolean isBrokerOf = (Boolean) response.get("data");
            log.debug("Broker {} is broker of customer {}: {}", brokerId, customerId, isBrokerOf);

            return Boolean.TRUE.equals(isBrokerOf);
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("Broker-customer relationship not found: broker={}, customer={}", brokerId, customerId);
            return false;
        } catch (Exception e) {
            log.error("Error checking broker-customer relationship: broker={}, customer={}, error={}",
                    brokerId, customerId, e.getMessage());
            // Fail closed for security
            return false;
        }
    }
}
