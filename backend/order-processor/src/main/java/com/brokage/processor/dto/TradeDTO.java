package com.brokage.processor.dto;

import com.brokage.common.enums.OrderSide;
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
public class TradeDTO {

    private UUID id;
    private UUID buyOrderId;
    private UUID sellOrderId;
    private UUID buyerCustomerId;
    private UUID sellerCustomerId;
    private String assetSymbol;
    private BigDecimal quantity;
    private BigDecimal price;
    private BigDecimal totalValue;
    private OrderSide takerSide;
    private LocalDateTime createdAt;
}
