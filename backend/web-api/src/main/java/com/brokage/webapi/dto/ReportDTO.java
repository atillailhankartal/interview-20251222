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
public class ReportDTO {

    private String reportId;
    private String reportType;
    private String reportName;
    private LocalDateTime generatedAt;
    private String generatedBy;
    private LocalDate startDate;
    private LocalDate endDate;
    private Map<String, Object> parameters;
    private Object data;

    public enum ReportType {
        DAILY_TRADING_SUMMARY,
        WEEKLY_TRADING_SUMMARY,
        MONTHLY_TRADING_SUMMARY,
        CUSTOMER_PORTFOLIO,
        CUSTOMER_TRANSACTION_HISTORY,
        BROKER_PERFORMANCE,
        SYSTEM_AUDIT_REPORT,
        PNL_REPORT,
        ASSET_DISTRIBUTION
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TradingSummaryReport {
        private LocalDate date;
        private long totalOrders;
        private long buyOrders;
        private long sellOrders;
        private long matchedOrders;
        private long cancelledOrders;
        private BigDecimal totalVolume;
        private BigDecimal buyVolume;
        private BigDecimal sellVolume;
        private Map<String, AssetSummary> assetSummaries;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssetSummary {
        private String assetSymbol;
        private long orderCount;
        private BigDecimal totalVolume;
        private BigDecimal averagePrice;
        private BigDecimal highPrice;
        private BigDecimal lowPrice;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerPortfolioReport {
        private String customerId;
        private String customerName;
        private LocalDateTime reportDate;
        private BigDecimal totalPortfolioValue;
        private BigDecimal tryBalance;
        private BigDecimal tryUsable;
        private BigDecimal tryBlocked;
        private List<AssetHolding> holdings;
        private PnLSummary pnlSummary;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssetHolding {
        private String assetSymbol;
        private BigDecimal quantity;
        private BigDecimal usableQuantity;
        private BigDecimal blockedQuantity;
        private BigDecimal currentPrice;
        private BigDecimal marketValue;
        private BigDecimal unrealizedPnL;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PnLSummary {
        private BigDecimal dailyPnL;
        private BigDecimal weeklyPnL;
        private BigDecimal monthlyPnL;
        private BigDecimal yearlyPnL;
        private BigDecimal totalRealizedPnL;
        private BigDecimal totalUnrealizedPnL;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BrokerPerformanceReport {
        private String brokerId;
        private String brokerName;
        private LocalDate startDate;
        private LocalDate endDate;
        private long assignedCustomers;
        private long activeCustomers;
        private BigDecimal totalCustomerVolume;
        private long totalCustomerOrders;
        private BigDecimal customerAssetsUnderManagement;
        private List<CustomerSummary> topCustomers;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerSummary {
        private String customerId;
        private String customerName;
        private BigDecimal tradingVolume;
        private long orderCount;
        private BigDecimal portfolioValue;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransactionHistoryReport {
        private String customerId;
        private LocalDate startDate;
        private LocalDate endDate;
        private List<TransactionRecord> transactions;
        private BigDecimal totalDeposits;
        private BigDecimal totalWithdrawals;
        private BigDecimal netFlow;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransactionRecord {
        private String transactionId;
        private String type; // ORDER, DEPOSIT, WITHDRAWAL
        private String assetSymbol;
        private String side; // BUY, SELL, DEPOSIT, WITHDRAW
        private BigDecimal amount;
        private BigDecimal price;
        private BigDecimal total;
        private String status;
        private LocalDateTime timestamp;
    }
}
