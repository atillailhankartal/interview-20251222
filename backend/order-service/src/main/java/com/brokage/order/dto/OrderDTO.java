package com.brokage.order.dto;

import com.brokage.common.enums.OrderSide;
import com.brokage.common.enums.OrderStatus;
import com.brokage.common.enums.OrderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDTO {

    private UUID id;
    private UUID customerId;
    private String assetSymbol;
    private OrderSide orderSide;
    private OrderType orderType;
    private BigDecimal size;
    private BigDecimal price;
    private BigDecimal filledSize;
    private BigDecimal remainingSize;
    private BigDecimal totalValue;
    private OrderStatus status;
    private String rejectionReason;
    private Integer customerTierPriority;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime matchedAt;
    private LocalDateTime cancelledAt;
}
