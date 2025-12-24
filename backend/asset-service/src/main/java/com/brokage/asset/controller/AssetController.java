package com.brokage.asset.controller;

import com.brokage.asset.dto.CustomerAssetDTO;
import com.brokage.asset.dto.DepositWithdrawRequest;
import com.brokage.asset.service.AssetService;
import com.brokage.common.dto.ApiResponse;
import com.brokage.common.dto.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/assets")
@RequiredArgsConstructor
@Slf4j
public class AssetController {

    private final AssetService assetService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'BROKER', 'CUSTOMER')")
    public ResponseEntity<ApiResponse<List<CustomerAssetDTO>>> getAssets(
            @RequestParam(required = false) UUID customerId,
            @AuthenticationPrincipal Jwt jwt) {

        UUID targetCustomerId = resolveCustomerId(customerId, jwt);
        log.debug("Getting assets for customer: {}", targetCustomerId);

        List<CustomerAssetDTO> assets = assetService.getCustomerAssets(targetCustomerId);

        return ResponseEntity.ok(ApiResponse.success(assets));
    }

    @GetMapping("/paged")
    @PreAuthorize("hasAnyRole('ADMIN', 'BROKER', 'CUSTOMER')")
    public ResponseEntity<ApiResponse<PageResponse<CustomerAssetDTO>>> getAssetsPaged(
            @RequestParam(required = false) UUID customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal Jwt jwt) {

        UUID targetCustomerId = resolveCustomerId(customerId, jwt);
        log.debug("Getting paged assets for customer: {}, page: {}, size: {}", targetCustomerId, page, size);

        PageResponse<CustomerAssetDTO> assets = assetService.getCustomerAssetsPaged(targetCustomerId, page, size);

        return ResponseEntity.ok(ApiResponse.success(assets));
    }

    @GetMapping("/{assetSymbol}")
    @PreAuthorize("hasAnyRole('ADMIN', 'BROKER', 'CUSTOMER')")
    public ResponseEntity<ApiResponse<CustomerAssetDTO>> getAsset(
            @PathVariable String assetSymbol,
            @RequestParam(required = false) UUID customerId,
            @AuthenticationPrincipal Jwt jwt) {

        UUID targetCustomerId = resolveCustomerId(customerId, jwt);
        log.debug("Getting asset {} for customer: {}", assetSymbol, targetCustomerId);

        CustomerAssetDTO asset = assetService.getCustomerAsset(targetCustomerId, assetSymbol);

        return ResponseEntity.ok(ApiResponse.success(asset));
    }

    @PostMapping("/deposit")
    @PreAuthorize("hasAnyRole('ADMIN', 'BROKER')")
    public ResponseEntity<ApiResponse<CustomerAssetDTO>> deposit(
            @Valid @RequestBody DepositWithdrawRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        log.info("Deposit request: customerId={}, asset={}, amount={}",
                request.getCustomerId(), request.getAssetSymbol(), request.getAmount());

        CustomerAssetDTO result = assetService.deposit(request);

        return ResponseEntity.ok(ApiResponse.success(result, "Deposit completed successfully"));
    }

    @PostMapping("/withdraw")
    @PreAuthorize("hasAnyRole('ADMIN', 'BROKER')")
    public ResponseEntity<ApiResponse<CustomerAssetDTO>> withdraw(
            @Valid @RequestBody DepositWithdrawRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        log.info("Withdrawal request: customerId={}, asset={}, amount={}",
                request.getCustomerId(), request.getAssetSymbol(), request.getAmount());

        CustomerAssetDTO result = assetService.withdraw(request);

        return ResponseEntity.ok(ApiResponse.success(result, "Withdrawal completed successfully"));
    }

    private UUID resolveCustomerId(UUID requestedCustomerId, Jwt jwt) {
        boolean isAdmin = hasRole(jwt, "ADMIN");
        boolean isBroker = hasRole(jwt, "BROKER");
        UUID jwtCustomerId = UUID.fromString(jwt.getSubject());

        if (isAdmin || isBroker) {
            return requestedCustomerId != null ? requestedCustomerId : jwtCustomerId;
        }

        // Customer can only access their own assets
        if (requestedCustomerId != null && !requestedCustomerId.equals(jwtCustomerId)) {
            log.warn("Customer {} attempted to access assets of customer {}",
                    jwtCustomerId, requestedCustomerId);
            throw new org.springframework.security.access.AccessDeniedException(
                    "You can only access your own assets");
        }

        return jwtCustomerId;
    }

    private boolean hasRole(Jwt jwt, String role) {
        var authorities = jwt.getClaimAsStringList("roles");
        if (authorities != null) {
            return authorities.contains(role);
        }

        var realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess != null) {
            var roles = realmAccess.get("roles");
            if (roles instanceof List<?> roleList) {
                return roleList.contains(role);
            }
        }

        return false;
    }
}
