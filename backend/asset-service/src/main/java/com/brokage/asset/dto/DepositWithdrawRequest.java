package com.brokage.asset.dto;

import jakarta.validation.constraints.*;
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
public class DepositWithdrawRequest {

    // Optional - for CUSTOMER role, will be resolved from JWT token
    // Required for ADMIN role
    private UUID customerId;

    @NotBlank(message = "Asset name is required")
    @Size(max = 20, message = "Asset name must not exceed 20 characters")
    private String assetName;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.0001", message = "Amount must be greater than 0")
    @Digits(integer = 15, fraction = 4, message = "Amount format is invalid")
    private BigDecimal amount;
}
