package com.brokage.order.dto;

import com.brokage.common.enums.OrderSide;
import com.brokage.common.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderFilterRequest {

    private UUID customerId;
    private String assetName;
    private OrderSide orderSide;
    private OrderStatus status;
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    @Builder.Default
    private Integer page = 0;

    @Builder.Default
    private Integer size = 20;

    @Builder.Default
    private String sortBy = "createdAt";

    @Builder.Default
    private String sortDirection = "DESC";
}
