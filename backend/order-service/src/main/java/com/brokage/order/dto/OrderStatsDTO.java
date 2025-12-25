package com.brokage.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatsDTO {
    private long pendingOrders;
    private long matchedOrders;
    private long cancelledOrders;
    private long totalOrders;
    private BigDecimal totalVolume;
    private List<OrderDTO> recentOrders;
}
