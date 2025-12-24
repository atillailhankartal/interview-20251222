package com.brokage.processor.dto;

import com.brokage.common.enums.OrderSide;
import com.brokage.processor.entity.MatchingQueue.QueueStatus;
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
public class MatchingQueueDTO {

    private UUID id;
    private UUID orderId;
    private UUID customerId;
    private String assetSymbol;
    private OrderSide orderSide;
    private BigDecimal price;
    private BigDecimal remainingSize;
    private Integer tierPriority;
    private QueueStatus status;
    private LocalDateTime queuedAt;
    private LocalDateTime matchedAt;
}
