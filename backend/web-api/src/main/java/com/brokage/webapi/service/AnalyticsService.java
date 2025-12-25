package com.brokage.webapi.service;

import com.brokage.webapi.dto.AnalyticsDTO;
import com.brokage.webapi.dto.AnalyticsDTO.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {

    private final WebClient orderServiceClient;
    private final WebClient assetServiceClient;
    private final WebClient customerServiceClient;

    /**
     * Get comprehensive analytics based on user role
     */
    public Mono<AnalyticsDTO> getAnalytics(Authentication authentication, String token, String period) {
        String role = extractPrimaryRole(authentication);

        return switch (role) {
            case "ADMIN" -> buildAdminAnalytics(token, period);
            case "BROKER" -> buildBrokerAnalytics(authentication, token, period);
            case "CUSTOMER" -> buildCustomerAnalytics(authentication, token, period);
            default -> buildCustomerAnalytics(authentication, token, period);
        };
    }

    private Mono<AnalyticsDTO> buildAdminAnalytics(String token, String period) {
        return Mono.zip(
                fetchTradingAnalytics(token),
                fetchCustomerAnalytics(token),
                fetchAssetAnalytics(token),
                fetchPerformanceMetrics(token)
        ).map(tuple -> AnalyticsDTO.builder()
                .generatedAt(LocalDateTime.now())
                .period(period)
                .tradingAnalytics(tuple.getT1())
                .customerAnalytics(tuple.getT2())
                .assetAnalytics(tuple.getT3())
                .performanceMetrics(tuple.getT4())
                .build()
        ).onErrorResume(e -> {
            log.error("Error building admin analytics", e);
            return Mono.just(createEmptyAnalytics(period));
        });
    }

    private Mono<AnalyticsDTO> buildBrokerAnalytics(Authentication authentication, String token, String period) {
        String brokerId = extractUserId(authentication);

        return Mono.zip(
                fetchBrokerTradingAnalytics(brokerId, token),
                fetchBrokerCustomerAnalytics(brokerId, token)
        ).map(tuple -> AnalyticsDTO.builder()
                .generatedAt(LocalDateTime.now())
                .period(period)
                .tradingAnalytics(tuple.getT1())
                .customerAnalytics(tuple.getT2())
                .build()
        ).onErrorResume(e -> {
            log.error("Error building broker analytics", e);
            return Mono.just(createEmptyAnalytics(period));
        });
    }

    private Mono<AnalyticsDTO> buildCustomerAnalytics(Authentication authentication, String token, String period) {
        String customerId = extractUserId(authentication);

        return fetchCustomerTradingAnalytics(customerId, token)
                .map(tradingAnalytics -> AnalyticsDTO.builder()
                        .generatedAt(LocalDateTime.now())
                        .period(period)
                        .tradingAnalytics(tradingAnalytics)
                        .build()
                ).onErrorResume(e -> {
                    log.error("Error building customer analytics", e);
                    return Mono.just(createEmptyAnalytics(period));
                });
    }

    private Mono<TradingAnalytics> fetchTradingAnalytics(String token) {
        return orderServiceClient.get()
                .uri("/api/orders/stats")
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(Map.class)
                .map(this::mapToTradingAnalytics)
                .onErrorReturn(createEmptyTradingAnalytics());
    }

    private Mono<TradingAnalytics> fetchBrokerTradingAnalytics(String brokerId, String token) {
        return orderServiceClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/orders/stats")
                        .queryParam("brokerId", brokerId)
                        .build())
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(Map.class)
                .map(this::mapToTradingAnalytics)
                .onErrorReturn(createEmptyTradingAnalytics());
    }

    private Mono<TradingAnalytics> fetchCustomerTradingAnalytics(String customerId, String token) {
        return orderServiceClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/orders/stats")
                        .queryParam("customerId", customerId)
                        .build())
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(Map.class)
                .map(this::mapToTradingAnalytics)
                .onErrorReturn(createEmptyTradingAnalytics());
    }

    private Mono<CustomerAnalytics> fetchCustomerAnalytics(String token) {
        return customerServiceClient.get()
                .uri("/api/customers/stats")
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(Map.class)
                .map(this::mapToCustomerAnalytics)
                .onErrorReturn(createEmptyCustomerAnalytics());
    }

    private Mono<CustomerAnalytics> fetchBrokerCustomerAnalytics(String brokerId, String token) {
        return customerServiceClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/broker-customers/{brokerId}/stats")
                        .build(brokerId))
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(Map.class)
                .map(this::mapToCustomerAnalytics)
                .onErrorReturn(createEmptyCustomerAnalytics());
    }

    private Mono<AssetAnalytics> fetchAssetAnalytics(String token) {
        return assetServiceClient.get()
                .uri("/api/assets/stats")
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(Map.class)
                .map(this::mapToAssetAnalytics)
                .onErrorReturn(createEmptyAssetAnalytics());
    }

    private Mono<PerformanceMetrics> fetchPerformanceMetrics(String token) {
        return Mono.zip(
                checkServiceHealth(orderServiceClient, "order-service"),
                checkServiceHealth(assetServiceClient, "asset-service"),
                checkServiceHealth(customerServiceClient, "customer-service")
        ).map(tuple -> PerformanceMetrics.builder()
                .orderFillRate(0.85) // TODO: Calculate from actual data
                .averageMatchTime(150.0) // TODO: Calculate from actual data
                .systemUptime(System.currentTimeMillis())
                .serviceHealthMap(Map.of(
                        "order-service", tuple.getT1(),
                        "asset-service", tuple.getT2(),
                        "customer-service", tuple.getT3()
                ))
                .build());
    }

    private Mono<ServiceHealth> checkServiceHealth(WebClient client, String serviceName) {
        long startTime = System.currentTimeMillis();
        return client.get()
                .uri("/actuator/health")
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> ServiceHealth.builder()
                        .serviceName(serviceName)
                        .status("UP")
                        .responseTimeMs(System.currentTimeMillis() - startTime)
                        .lastCheck(LocalDateTime.now())
                        .build())
                .onErrorReturn(ServiceHealth.builder()
                        .serviceName(serviceName)
                        .status("DOWN")
                        .responseTimeMs(-1)
                        .lastCheck(LocalDateTime.now())
                        .build());
    }

    @SuppressWarnings("unchecked")
    private TradingAnalytics mapToTradingAnalytics(Map<String, Object> data) {
        Map<String, Object> statsData = (Map<String, Object>) data.getOrDefault("data", data);
        return TradingAnalytics.builder()
                .totalOrders(getLong(statsData, "totalOrders"))
                .pendingOrders(getLong(statsData, "pendingOrders"))
                .matchedOrders(getLong(statsData, "matchedOrders"))
                .cancelledOrders(getLong(statsData, "cancelledOrders"))
                .totalTradingVolume(getBigDecimal(statsData, "totalVolume"))
                .averageOrderSize(getBigDecimal(statsData, "averageOrderSize"))
                .dailyVolumes(List.of())
                .ordersByAsset(Map.of())
                .ordersByStatus(Map.of())
                .build();
    }

    @SuppressWarnings("unchecked")
    private CustomerAnalytics mapToCustomerAnalytics(Map<String, Object> data) {
        Map<String, Object> statsData = (Map<String, Object>) data.getOrDefault("data", data);
        return CustomerAnalytics.builder()
                .totalCustomers(getLong(statsData, "totalCustomers"))
                .activeCustomers(getLong(statsData, "activeCustomers"))
                .newCustomersThisPeriod(getLong(statsData, "newCustomers"))
                .customersByTier(Map.of())
                .topCustomersByVolume(List.of())
                .build();
    }

    @SuppressWarnings("unchecked")
    private AssetAnalytics mapToAssetAnalytics(Map<String, Object> data) {
        Map<String, Object> statsData = (Map<String, Object>) data.getOrDefault("data", data);
        return AssetAnalytics.builder()
                .totalAssetsUnderManagement(getBigDecimal(statsData, "totalAUM"))
                .totalTryBalance(getBigDecimal(statsData, "totalTryBalance"))
                .assetDistribution(Map.of())
                .topPerformingAssets(List.of())
                .build();
    }

    private AnalyticsDTO createEmptyAnalytics(String period) {
        return AnalyticsDTO.builder()
                .generatedAt(LocalDateTime.now())
                .period(period)
                .tradingAnalytics(createEmptyTradingAnalytics())
                .customerAnalytics(createEmptyCustomerAnalytics())
                .assetAnalytics(createEmptyAssetAnalytics())
                .build();
    }

    private TradingAnalytics createEmptyTradingAnalytics() {
        return TradingAnalytics.builder()
                .totalOrders(0L)
                .pendingOrders(0L)
                .matchedOrders(0L)
                .cancelledOrders(0L)
                .totalTradingVolume(BigDecimal.ZERO)
                .averageOrderSize(BigDecimal.ZERO)
                .dailyVolumes(List.of())
                .ordersByAsset(Map.of())
                .ordersByStatus(Map.of())
                .build();
    }

    private CustomerAnalytics createEmptyCustomerAnalytics() {
        return CustomerAnalytics.builder()
                .totalCustomers(0L)
                .activeCustomers(0L)
                .newCustomersThisPeriod(0L)
                .customersByTier(Map.of())
                .topCustomersByVolume(List.of())
                .build();
    }

    private AssetAnalytics createEmptyAssetAnalytics() {
        return AssetAnalytics.builder()
                .totalAssetsUnderManagement(BigDecimal.ZERO)
                .totalTryBalance(BigDecimal.ZERO)
                .assetDistribution(Map.of())
                .topPerformingAssets(List.of())
                .build();
    }

    private String extractPrimaryRole(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .filter(role -> List.of("ADMIN", "BROKER", "CUSTOMER").contains(role))
                .findFirst()
                .orElse("CUSTOMER");
    }

    private String extractUserId(Authentication authentication) {
        if (authentication.getPrincipal() instanceof org.springframework.security.oauth2.jwt.Jwt jwt) {
            return jwt.getSubject();
        }
        return "unknown";
    }

    private long getLong(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return 0L;
    }

    private BigDecimal getBigDecimal(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        return BigDecimal.ZERO;
    }
}
