package com.brokage.asset.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReserveAssetRequest {
    @NotNull(message = "Customer ID is required")
    private UUID customerId;

    @NotBlank(message = "Asset name is required")
    private String assetName;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.0001", message = "Amount must be greater than 0")
    private BigDecimal amount;

    /**
     * Order ID for idempotency - if same orderId tries to reserve twice, second call is idempotent
     */
    private UUID orderId;
}
