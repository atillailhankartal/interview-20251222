package com.brokage.webapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsDTO {

    private LocalDateTime generatedAt;
    private String period; // DAY, WEEK, MONTH, YEAR
    private TradingAnalytics tradingAnalytics;
    private CustomerAnalytics customerAnalytics;
    private AssetAnalytics assetAnalytics;
    private PerformanceMetrics performanceMetrics;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TradingAnalytics {
        private long totalOrders;
        private long pendingOrders;
        private long matchedOrders;
        private long cancelledOrders;
        private BigDecimal totalTradingVolume;
        private BigDecimal averageOrderSize;
        private List<DailyVolume> dailyVolumes;
        private Map<String, Long> ordersByAsset;
        private Map<String, Long> ordersByStatus;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerAnalytics {
        private long totalCustomers;
        private long activeCustomers;
        private long newCustomersThisPeriod;
        private Map<String, Long> customersByTier;
        private List<TopCustomer> topCustomersByVolume;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssetAnalytics {
        private BigDecimal totalAssetsUnderManagement;
        private BigDecimal totalTryBalance;
        private Map<String, BigDecimal> assetDistribution;
        private List<AssetPerformance> topPerformingAssets;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PerformanceMetrics {
        private double orderFillRate;
        private double averageMatchTime;
        private long systemUptime;
        private Map<String, ServiceHealth> serviceHealthMap;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyVolume {
        private LocalDate date;
        private long orderCount;
        private BigDecimal volume;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopCustomer {
        private String customerId;
        private String customerName;
        private BigDecimal tradingVolume;
        private long orderCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssetPerformance {
        private String assetSymbol;
        private BigDecimal totalVolume;
        private long tradeCount;
        private BigDecimal priceChange;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServiceHealth {
        private String serviceName;
        private String status;
        private long responseTimeMs;
        private LocalDateTime lastCheck;
    }
}
