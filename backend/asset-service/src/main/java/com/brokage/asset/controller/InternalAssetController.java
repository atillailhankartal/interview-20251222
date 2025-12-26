package com.brokage.asset.controller;

import com.brokage.asset.dto.CustomerAssetDTO;
import com.brokage.asset.dto.ReserveAssetRequest;
import com.brokage.asset.service.AssetService;
import com.brokage.common.dto.ApiResponse;
import com.brokage.common.exception.BusinessException;
import com.brokage.common.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Internal endpoints for service-to-service communication.
 * These endpoints are not exposed externally and are only accessible within the cluster.
 * Security is handled at network level (internal network only).
 */
@RestController
@RequestMapping("/internal/assets")
@RequiredArgsConstructor
@Slf4j
public class InternalAssetController {

    private final AssetService assetService;

    /**
     * Reserve asset balance for an order (sync validation).
     * Called by Order Service BEFORE creating an order.
     *
     * @param request Reserve request containing customerId, assetName, amount
     * @return Success if reservation successful, error otherwise
     */
    @PostMapping("/reserve")
    public ResponseEntity<ApiResponse<CustomerAssetDTO>> reserveAsset(
            @Valid @RequestBody ReserveAssetRequest request) {

        log.info("Internal reserve request: customerId={}, asset={}, amount={}, orderId={}",
                request.getCustomerId(), request.getAssetName(), request.getAmount(), request.getOrderId());

        try {
            CustomerAssetDTO result = assetService.reserveAsset(
                    request.getCustomerId(),
                    request.getAssetName(),
                    request.getAmount()
            );

            return ResponseEntity.ok(ApiResponse.success(result, "Asset reserved successfully"));

        } catch (ResourceNotFoundException e) {
            log.warn("Asset not found for reservation: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Asset not found: " + request.getAssetName()));

        } catch (BusinessException e) {
            log.warn("Insufficient balance for reservation: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Insufficient balance: " + e.getMessage()));
        }
    }

    /**
     * Release reserved asset (for order cancellation compensation).
     * Called when order creation fails after reservation.
     */
    @PostMapping("/release")
    public ResponseEntity<ApiResponse<CustomerAssetDTO>> releaseAsset(
            @Valid @RequestBody ReserveAssetRequest request) {

        log.info("Internal release request: customerId={}, asset={}, amount={}",
                request.getCustomerId(), request.getAssetName(), request.getAmount());

        try {
            CustomerAssetDTO result = assetService.releaseReservation(
                    request.getCustomerId(),
                    request.getAssetName(),
                    request.getAmount()
            );

            return ResponseEntity.ok(ApiResponse.success(result, "Asset released successfully"));

        } catch (Exception e) {
            log.error("Failed to release asset: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Release failed: " + e.getMessage()));
        }
    }

    /**
     * Check if customer has sufficient balance (without reserving).
     * Used for pre-validation before order creation.
     */
    @GetMapping("/balance/check")
    public ResponseEntity<ApiResponse<Boolean>> checkBalance(
            @RequestParam UUID customerId,
            @RequestParam String assetName,
            @RequestParam BigDecimal amount) {

        log.debug("Balance check: customerId={}, asset={}, amount={}", customerId, assetName, amount);

        try {
            BigDecimal usableBalance = assetService.getUsableBalance(customerId, assetName);
            boolean hasSufficientBalance = usableBalance != null && usableBalance.compareTo(amount) >= 0;

            return ResponseEntity.ok(ApiResponse.success(hasSufficientBalance));

        } catch (Exception e) {
            log.warn("Balance check failed: {}", e.getMessage());
            return ResponseEntity.ok(ApiResponse.success(false));
        }
    }
}
