package com.brokage.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

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
    private List<TopTraderDTO> topTraders;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopTraderDTO {
        private UUID customerId;
        private String customerName;
        private long orderCount;
        private BigDecimal tradingVolume;
    }
}
