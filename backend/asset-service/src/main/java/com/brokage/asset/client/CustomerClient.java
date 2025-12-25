package com.brokage.asset.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

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
}
