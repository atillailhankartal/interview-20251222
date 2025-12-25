package com.brokage.webapi.service;

import com.brokage.webapi.dto.ReportDTO;
import com.brokage.webapi.dto.ReportDTO.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportsService {

    private final WebClient orderServiceClient;
    private final WebClient assetServiceClient;
    private final WebClient customerServiceClient;

    /**
     * Generate daily trading summary report (ADMIN/BROKER)
     */
    public Mono<ReportDTO> generateDailyTradingSummary(LocalDate date, String token) {
        return orderServiceClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/orders")
                        .queryParam("startDate", date.atStartOfDay())
                        .queryParam("endDate", date.plusDays(1).atStartOfDay())
                        .build())
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(Map.class)
                .map(data -> {
                    TradingSummaryReport summary = mapToTradingSummary(data, date);
                    return ReportDTO.builder()
                            .reportId(UUID.randomUUID().toString())
                            .reportType("DAILY_TRADING_SUMMARY")
                            .reportName("Daily Trading Summary - " + date)
                            .generatedAt(LocalDateTime.now())
                            .startDate(date)
                            .endDate(date)
                            .data(summary)
                            .build();
                })
                .onErrorResume(e -> {
                    log.error("Error generating daily trading summary", e);
                    return Mono.just(createEmptyReport("DAILY_TRADING_SUMMARY", date, date));
                });
    }

    /**
     * Generate customer portfolio report
     */
    public Mono<ReportDTO> generateCustomerPortfolioReport(String customerId, String token) {
        return Mono.zip(
                fetchCustomerAssets(customerId, token),
                fetchCustomerOrders(customerId, token),
                fetchCustomerInfo(customerId, token)
        ).map(tuple -> {
            CustomerPortfolioReport portfolio = buildPortfolioReport(
                    customerId,
                    tuple.getT1(),
                    tuple.getT2(),
                    tuple.getT3()
            );
            return ReportDTO.builder()
                    .reportId(UUID.randomUUID().toString())
                    .reportType("CUSTOMER_PORTFOLIO")
                    .reportName("Portfolio Report - " + customerId)
                    .generatedAt(LocalDateTime.now())
                    .startDate(LocalDate.now())
                    .endDate(LocalDate.now())
                    .parameters(Map.of("customerId", customerId))
                    .data(portfolio)
                    .build();
        }).onErrorResume(e -> {
            log.error("Error generating portfolio report for customer: {}", customerId, e);
            return Mono.just(createEmptyReport("CUSTOMER_PORTFOLIO", LocalDate.now(), LocalDate.now()));
        });
    }

    /**
     * Generate transaction history report
     */
    public Mono<ReportDTO> generateTransactionHistory(String customerId, LocalDate startDate,
                                                       LocalDate endDate, String token) {
        return orderServiceClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/orders")
                        .queryParam("customerId", customerId)
                        .queryParam("startDate", startDate.atStartOfDay())
                        .queryParam("endDate", endDate.plusDays(1).atStartOfDay())
                        .build())
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(Map.class)
                .map(data -> {
                    TransactionHistoryReport history = mapToTransactionHistory(customerId, data, startDate, endDate);
                    return ReportDTO.builder()
                            .reportId(UUID.randomUUID().toString())
                            .reportType("CUSTOMER_TRANSACTION_HISTORY")
                            .reportName("Transaction History - " + customerId)
                            .generatedAt(LocalDateTime.now())
                            .startDate(startDate)
                            .endDate(endDate)
                            .parameters(Map.of("customerId", customerId))
                            .data(history)
                            .build();
                })
                .onErrorResume(e -> {
                    log.error("Error generating transaction history for customer: {}", customerId, e);
                    return Mono.just(createEmptyReport("CUSTOMER_TRANSACTION_HISTORY", startDate, endDate));
                });
    }

    /**
     * Generate broker performance report (ADMIN only)
     */
    public Mono<ReportDTO> generateBrokerPerformanceReport(String brokerId, LocalDate startDate,
                                                            LocalDate endDate, String token) {
        return Mono.zip(
                fetchBrokerCustomers(brokerId, token),
                fetchBrokerOrders(brokerId, startDate, endDate, token)
        ).map(tuple -> {
            BrokerPerformanceReport performance = buildBrokerPerformanceReport(
                    brokerId, tuple.getT1(), tuple.getT2(), startDate, endDate
            );
            return ReportDTO.builder()
                    .reportId(UUID.randomUUID().toString())
                    .reportType("BROKER_PERFORMANCE")
                    .reportName("Broker Performance - " + brokerId)
                    .generatedAt(LocalDateTime.now())
                    .startDate(startDate)
                    .endDate(endDate)
                    .parameters(Map.of("brokerId", brokerId))
                    .data(performance)
                    .build();
        }).onErrorResume(e -> {
            log.error("Error generating broker performance report for: {}", brokerId, e);
            return Mono.just(createEmptyReport("BROKER_PERFORMANCE", startDate, endDate));
        });
    }

    private Mono<Map<String, Object>> fetchCustomerAssets(String customerId, String token) {
        return assetServiceClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/assets")
                        .queryParam("customerId", customerId)
                        .build())
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .onErrorReturn(Map.of());
    }

    private Mono<Map<String, Object>> fetchCustomerOrders(String customerId, String token) {
        return orderServiceClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/orders")
                        .queryParam("customerId", customerId)
                        .build())
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .onErrorReturn(Map.of());
    }

    private Mono<Map<String, Object>> fetchCustomerInfo(String customerId, String token) {
        return customerServiceClient.get()
                .uri("/api/customers/" + customerId)
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .onErrorReturn(Map.of());
    }

    private Mono<Map<String, Object>> fetchBrokerCustomers(String brokerId, String token) {
        return customerServiceClient.get()
                .uri("/api/broker-customers/" + brokerId)
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .onErrorReturn(Map.of());
    }

    private Mono<Map<String, Object>> fetchBrokerOrders(String brokerId, LocalDate startDate,
                                                         LocalDate endDate, String token) {
        return orderServiceClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/orders")
                        .queryParam("brokerId", brokerId)
                        .queryParam("startDate", startDate.atStartOfDay())
                        .queryParam("endDate", endDate.plusDays(1).atStartOfDay())
                        .build())
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .onErrorReturn(Map.of());
    }

    @SuppressWarnings("unchecked")
    private TradingSummaryReport mapToTradingSummary(Map<String, Object> data, LocalDate date) {
        List<Map<String, Object>> orders = (List<Map<String, Object>>) data.getOrDefault("data", List.of());

        long buyOrders = orders.stream()
                .filter(o -> "BUY".equals(o.get("orderSide")))
                .count();
        long sellOrders = orders.stream()
                .filter(o -> "SELL".equals(o.get("orderSide")))
                .count();
        long matchedOrders = orders.stream()
                .filter(o -> "MATCHED".equals(o.get("status")))
                .count();
        long cancelledOrders = orders.stream()
                .filter(o -> "CANCELED".equals(o.get("status")))
                .count();

        BigDecimal totalVolume = orders.stream()
                .map(o -> getBigDecimal(o, "size").multiply(getBigDecimal(o, "price")))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return TradingSummaryReport.builder()
                .date(date)
                .totalOrders(orders.size())
                .buyOrders(buyOrders)
                .sellOrders(sellOrders)
                .matchedOrders(matchedOrders)
                .cancelledOrders(cancelledOrders)
                .totalVolume(totalVolume)
                .buyVolume(BigDecimal.ZERO) // TODO: Calculate
                .sellVolume(BigDecimal.ZERO) // TODO: Calculate
                .assetSummaries(Map.of())
                .build();
    }

    @SuppressWarnings("unchecked")
    private CustomerPortfolioReport buildPortfolioReport(String customerId,
                                                          Map<String, Object> assetsData,
                                                          Map<String, Object> ordersData,
                                                          Map<String, Object> customerData) {
        List<Map<String, Object>> assets = (List<Map<String, Object>>) assetsData.getOrDefault("data", List.of());

        BigDecimal tryBalance = BigDecimal.ZERO;
        BigDecimal tryUsable = BigDecimal.ZERO;
        List<AssetHolding> holdings = new java.util.ArrayList<>();

        for (Map<String, Object> asset : assets) {
            String symbol = (String) asset.get("assetSymbol");
            BigDecimal size = getBigDecimal(asset, "size");
            BigDecimal usableSize = getBigDecimal(asset, "usableSize");

            if ("TRY".equals(symbol)) {
                tryBalance = size;
                tryUsable = usableSize;
            } else {
                holdings.add(AssetHolding.builder()
                        .assetSymbol(symbol)
                        .quantity(size)
                        .usableQuantity(usableSize)
                        .blockedQuantity(size.subtract(usableSize))
                        .currentPrice(BigDecimal.ZERO) // TODO: Get from market
                        .marketValue(BigDecimal.ZERO)
                        .unrealizedPnL(BigDecimal.ZERO)
                        .build());
            }
        }

        Map<String, Object> customer = (Map<String, Object>) customerData.getOrDefault("data", Map.of());
        String customerName = (String) customer.getOrDefault("fullName", "Unknown");

        return CustomerPortfolioReport.builder()
                .customerId(customerId)
                .customerName(customerName)
                .reportDate(LocalDateTime.now())
                .totalPortfolioValue(tryBalance)
                .tryBalance(tryBalance)
                .tryUsable(tryUsable)
                .tryBlocked(tryBalance.subtract(tryUsable))
                .holdings(holdings)
                .pnlSummary(PnLSummary.builder()
                        .dailyPnL(BigDecimal.ZERO)
                        .weeklyPnL(BigDecimal.ZERO)
                        .monthlyPnL(BigDecimal.ZERO)
                        .yearlyPnL(BigDecimal.ZERO)
                        .totalRealizedPnL(BigDecimal.ZERO)
                        .totalUnrealizedPnL(BigDecimal.ZERO)
                        .build())
                .build();
    }

    @SuppressWarnings("unchecked")
    private TransactionHistoryReport mapToTransactionHistory(String customerId, Map<String, Object> data,
                                                              LocalDate startDate, LocalDate endDate) {
        List<Map<String, Object>> orders = (List<Map<String, Object>>) data.getOrDefault("data", List.of());

        List<TransactionRecord> transactions = orders.stream()
                .map(o -> TransactionRecord.builder()
                        .transactionId((String) o.get("id"))
                        .type("ORDER")
                        .assetSymbol((String) o.get("assetSymbol"))
                        .side((String) o.get("orderSide"))
                        .amount(getBigDecimal(o, "size"))
                        .price(getBigDecimal(o, "price"))
                        .total(getBigDecimal(o, "size").multiply(getBigDecimal(o, "price")))
                        .status((String) o.get("status"))
                        .build())
                .toList();

        return TransactionHistoryReport.builder()
                .customerId(customerId)
                .startDate(startDate)
                .endDate(endDate)
                .transactions(transactions)
                .totalDeposits(BigDecimal.ZERO)
                .totalWithdrawals(BigDecimal.ZERO)
                .netFlow(BigDecimal.ZERO)
                .build();
    }

    @SuppressWarnings("unchecked")
    private BrokerPerformanceReport buildBrokerPerformanceReport(String brokerId,
                                                                   Map<String, Object> customersData,
                                                                   Map<String, Object> ordersData,
                                                                   LocalDate startDate,
                                                                   LocalDate endDate) {
        List<Map<String, Object>> customers = (List<Map<String, Object>>) customersData.getOrDefault("data", List.of());
        List<Map<String, Object>> orders = (List<Map<String, Object>>) ordersData.getOrDefault("data", List.of());

        BigDecimal totalVolume = orders.stream()
                .map(o -> getBigDecimal(o, "size").multiply(getBigDecimal(o, "price")))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return BrokerPerformanceReport.builder()
                .brokerId(brokerId)
                .brokerName("Broker " + brokerId)
                .startDate(startDate)
                .endDate(endDate)
                .assignedCustomers(customers.size())
                .activeCustomers(customers.size()) // TODO: Calculate active
                .totalCustomerVolume(totalVolume)
                .totalCustomerOrders(orders.size())
                .customerAssetsUnderManagement(BigDecimal.ZERO)
                .topCustomers(List.of())
                .build();
    }

    private ReportDTO createEmptyReport(String reportType, LocalDate startDate, LocalDate endDate) {
        return ReportDTO.builder()
                .reportId(UUID.randomUUID().toString())
                .reportType(reportType)
                .reportName(reportType + " Report")
                .generatedAt(LocalDateTime.now())
                .startDate(startDate)
                .endDate(endDate)
                .data(Map.of())
                .build();
    }

    private BigDecimal getBigDecimal(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        return BigDecimal.ZERO;
    }
}
