package com.brokage.order.client;

import com.brokage.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Client for communicating with Asset Service.
 * Used for sync balance validation before order creation.
 */
@Component
@Slf4j
public class AssetClient {

    private final RestTemplate restTemplate;
    private final String assetServiceUrl;

    public AssetClient(
            RestTemplate restTemplate,
            @Value("${services.asset-service.url}") String assetServiceUrl) {
        this.restTemplate = restTemplate;
        this.assetServiceUrl = assetServiceUrl;
    }

    /**
     * Reserve asset balance for an order.
     * Called BEFORE creating an order to ensure sufficient balance.
     *
     * @param customerId Customer ID
     * @param assetName Asset to reserve (TRY for BUY, stock for SELL)
     * @param amount Amount to reserve
     * @param orderId Order ID for idempotency (optional)
     * @return true if reservation successful
     * @throws BusinessException if insufficient balance or asset not found
     */
    public boolean reserveAsset(UUID customerId, String assetName, BigDecimal amount, UUID orderId) {
        try {
            String url = assetServiceUrl + "/internal/assets/reserve";
            log.info("Reserving asset: customerId={}, asset={}, amount={}, orderId={}",
                    customerId, assetName, amount, orderId);

            Map<String, Object> request = new HashMap<>();
            request.put("customerId", customerId.toString());
            request.put("assetName", assetName);
            request.put("amount", amount.toString());
            if (orderId != null) {
                request.put("orderId", orderId.toString());
            }

            @SuppressWarnings("unchecked")
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Boolean success = (Boolean) response.getBody().get("success");
                if (Boolean.TRUE.equals(success)) {
                    log.info("Asset reserved successfully: customerId={}, asset={}, amount={}",
                            customerId, assetName, amount);
                    return true;
                }

                // Extract error message
                String errorMessage = (String) response.getBody().get("message");
                log.warn("Asset reservation failed: {}", errorMessage);
                throw new BusinessException(errorMessage != null ? errorMessage : "Asset reservation failed");
            }

            return false;

        } catch (HttpClientErrorException.BadRequest e) {
            // Parse error response
            log.warn("Asset reservation rejected: {}", e.getResponseBodyAsString());
            throw new BusinessException("Insufficient balance or asset not found");

        } catch (BusinessException e) {
            throw e;

        } catch (Exception e) {
            log.error("Error reserving asset: customerId={}, asset={}, error={}",
                    customerId, assetName, e.getMessage());
            throw new BusinessException("Failed to validate balance: " + e.getMessage());
        }
    }

    /**
     * Release reserved asset (compensation for failed order creation).
     *
     * @param customerId Customer ID
     * @param assetName Asset to release
     * @param amount Amount to release
     */
    public void releaseAsset(UUID customerId, String assetName, BigDecimal amount) {
        try {
            String url = assetServiceUrl + "/internal/assets/release";
            log.info("Releasing asset: customerId={}, asset={}, amount={}", customerId, assetName, amount);

            Map<String, Object> request = new HashMap<>();
            request.put("customerId", customerId.toString());
            request.put("assetName", assetName);
            request.put("amount", amount.toString());

            restTemplate.postForEntity(url, request, Map.class);

            log.info("Asset released successfully: customerId={}, asset={}, amount={}",
                    customerId, assetName, amount);

        } catch (Exception e) {
            // Log but don't throw - this is compensation, best effort
            log.error("Failed to release asset (compensation): customerId={}, asset={}, error={}",
                    customerId, assetName, e.getMessage());
        }
    }

    /**
     * Check if customer has sufficient balance (without reserving).
     *
     * @param customerId Customer ID
     * @param assetName Asset to check
     * @param amount Required amount
     * @return true if sufficient balance
     */
    public boolean hasBalance(UUID customerId, String assetName, BigDecimal amount) {
        try {
            String url = String.format("%s/internal/assets/balance/check?customerId=%s&assetName=%s&amount=%s",
                    assetServiceUrl, customerId, assetName, amount);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response != null) {
                Boolean hasBalance = (Boolean) response.get("data");
                return Boolean.TRUE.equals(hasBalance);
            }

            return false;

        } catch (Exception e) {
            log.error("Error checking balance: customerId={}, asset={}, error={}",
                    customerId, assetName, e.getMessage());
            return false;
        }
    }
}
