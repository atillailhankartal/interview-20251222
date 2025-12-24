package com.brokage.order.dto;

import com.brokage.common.enums.OrderSide;
import com.brokage.common.enums.OrderType;
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
public class CreateOrderRequest {

    @NotNull(message = "Customer ID is required")
    private UUID customerId;

    @NotBlank(message = "Asset symbol is required")
    @Size(max = 20, message = "Asset symbol must not exceed 20 characters")
    private String assetSymbol;

    @NotNull(message = "Order side is required")
    private OrderSide orderSide;

    private OrderType orderType;

    @NotNull(message = "Size is required")
    @DecimalMin(value = "0.0001", message = "Size must be greater than 0")
    @Digits(integer = 15, fraction = 4, message = "Size format is invalid")
    private BigDecimal size;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0001", message = "Price must be greater than 0")
    @Digits(integer = 15, fraction = 4, message = "Price format is invalid")
    private BigDecimal price;

    @Size(max = 64, message = "Idempotency key must not exceed 64 characters")
    private String idempotencyKey;
}
