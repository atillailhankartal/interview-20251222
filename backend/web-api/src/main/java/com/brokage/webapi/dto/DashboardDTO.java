package com.brokage.webapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardDTO {
    private String role;
    private String userId;
    private String username;
    private LocalDateTime timestamp;

    // Common stats
    private OrderStats orderStats;
    private AssetStats assetStats;

    // Role-specific data
    private AdminDashboard adminData;
    private BrokerDashboard brokerData;
    private CustomerDashboard customerData;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderStats {
        private long pendingOrders;
        private long matchedOrders;
        private long cancelledOrders;
        private long totalOrders;
        private BigDecimal totalVolume;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssetStats {
        private int totalAssets;
        private BigDecimal tryBalance;
        private BigDecimal tryUsable;
        private BigDecimal tryBlocked;
        private BigDecimal portfolioValue;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdminDashboard {
        private long totalCustomers;
        private long totalBrokers;
        private long activeUsers;
        private BigDecimal totalTradingVolume;
        private List<TopTrader> topTraders;
        private List<RecentOrder> recentOrders;
        private SystemHealth systemHealth;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BrokerDashboard {
        private long assignedCustomers;
        private long activeCustomers;
        private BigDecimal customersPortfolioValue;
        private List<CustomerSummary> customerList;
        private List<RecentOrder> customerOrders;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerDashboard {
        private String brokerId;
        private String brokerName;
        private BigDecimal portfolioValue;
        private BigDecimal dailyPnL;
        private BigDecimal weeklyPnL;
        private List<AssetHolding> holdings;
        private List<RecentOrder> myOrders;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopTrader {
        private String customerId;
        private String customerName;
        private long orderCount;
        private BigDecimal tradingVolume;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentOrder {
        private String id;
        private String customerId;
        private String customerName;
        private String assetName;
        private String orderSide;
        private BigDecimal size;
        private BigDecimal price;
        private String status;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerSummary {
        private String customerId;
        private String name;
        private String email;
        private long orderCount;
        private BigDecimal portfolioValue;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssetHolding {
        private String assetName;
        private BigDecimal size;
        private BigDecimal usableSize;
        private BigDecimal currentPrice;
        private BigDecimal value;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SystemHealth {
        private String orderService;
        private String assetService;
        private String customerService;
        private String notificationService;
        private String auditService;
        private String kafkaStatus;
        private String redisStatus;
    }
}
