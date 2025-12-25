package com.brokage.webapi.service;

import com.brokage.webapi.dto.DashboardDTO;
import com.brokage.webapi.dto.DashboardDTO.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
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
public class DashboardService {

    private final WebClient orderServiceClient;
    private final WebClient assetServiceClient;
    private final WebClient customerServiceClient;

    public Mono<DashboardDTO> getDashboard(Authentication authentication, String token) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        String userId = jwt.getSubject();
        String username = jwt.getClaimAsString("preferred_username");
        String role = extractPrimaryRole(authentication);

        log.info("Building dashboard for user: {} with role: {}", username, role);

        return switch (role) {
            case "ADMIN" -> buildAdminDashboard(userId, username, token);
            case "BROKER" -> buildBrokerDashboard(userId, username, token);
            case "CUSTOMER" -> buildCustomerDashboard(userId, username, token);
            default -> buildCustomerDashboard(userId, username, token);
        };
    }

    private Mono<DashboardDTO> buildAdminDashboard(String userId, String username, String token) {
        return Mono.zip(
                fetchOrderStatsWithDetails(token),
                fetchAdminAssetStats(token),
                fetchSystemHealth(token),
                fetchCustomerStats(token)
        ).map(tuple -> {
            Map<String, Object> orderData = tuple.getT1();
            AssetStats assetStats = tuple.getT2();
            SystemHealth systemHealth = tuple.getT3();
            Map<String, Object> customerData = tuple.getT4();

            OrderStats orderStats = mapToOrderStats(orderData);
            List<RecentOrder> recentOrders = mapToRecentOrders(orderData);

            long totalCustomers = getLong(customerData, "totalCustomers");
            long totalBrokers = getLong(customerData, "totalBrokers");
            long activeUsers = getLong(customerData, "activeUsers");

            return DashboardDTO.builder()
                    .role("ADMIN")
                    .userId(userId)
                    .username(username)
                    .timestamp(LocalDateTime.now())
                    .orderStats(orderStats)
                    .assetStats(assetStats)
                    .adminData(AdminDashboard.builder()
                            .totalCustomers(totalCustomers)
                            .totalBrokers(totalBrokers)
                            .activeUsers(activeUsers)
                            .totalTradingVolume(orderStats.getTotalVolume())
                            .topTraders(List.of()) // TODO: Implement top traders
                            .recentOrders(recentOrders)
                            .systemHealth(systemHealth)
                            .build())
                    .build();
        }).onErrorResume(e -> {
            log.error("Error building admin dashboard", e);
            return Mono.just(createEmptyDashboard("ADMIN", userId, username));
        });
    }

    private Mono<DashboardDTO> buildBrokerDashboard(String userId, String username, String token) {
        return Mono.zip(
                fetchOrderStatsForBroker(userId, token),
                fetchBrokerAssetStats(userId, token)
        ).map(tuple -> {
            OrderStats orderStats = tuple.getT1();
            AssetStats assetStats = tuple.getT2();

            return DashboardDTO.builder()
                    .role("BROKER")
                    .userId(userId)
                    .username(username)
                    .timestamp(LocalDateTime.now())
                    .orderStats(orderStats)
                    .assetStats(assetStats)
                    .brokerData(BrokerDashboard.builder()
                            .assignedCustomers(0L)
                            .activeCustomers(0L)
                            .customersPortfolioValue(BigDecimal.ZERO)
                            .customerList(List.of())
                            .customerOrders(List.of())
                            .build())
                    .build();
        }).onErrorResume(e -> {
            log.error("Error building broker dashboard", e);
            return Mono.just(createEmptyDashboard("BROKER", userId, username));
        });
    }

    private Mono<DashboardDTO> buildCustomerDashboard(String userId, String username, String token) {
        return Mono.zip(
                fetchOrderStatsForCustomer(userId, token),
                fetchCustomerAssetStats(userId, token)
        ).map(tuple -> {
            OrderStats orderStats = tuple.getT1();
            AssetStats assetStats = tuple.getT2();

            return DashboardDTO.builder()
                    .role("CUSTOMER")
                    .userId(userId)
                    .username(username)
                    .timestamp(LocalDateTime.now())
                    .orderStats(orderStats)
                    .assetStats(assetStats)
                    .customerData(CustomerDashboard.builder()
                            .portfolioValue(assetStats.getPortfolioValue())
                            .dailyPnL(BigDecimal.ZERO)
                            .weeklyPnL(BigDecimal.ZERO)
                            .holdings(List.of())
                            .myOrders(List.of())
                            .build())
                    .build();
        }).onErrorResume(e -> {
            log.error("Error building customer dashboard", e);
            return Mono.just(createEmptyDashboard("CUSTOMER", userId, username));
        });
    }

    private Mono<OrderStats> fetchOrderStats(String token) {
        return orderServiceClient.get()
                .uri("/api/orders/stats")
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(Map.class)
                .map(this::mapToOrderStats)
                .onErrorReturn(createEmptyOrderStats());
    }

    @SuppressWarnings("unchecked")
    private Mono<Map<String, Object>> fetchOrderStatsWithDetails(String token) {
        return orderServiceClient.get()
                .uri("/api/orders/stats")
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    Object data = response.get("data");
                    if (data instanceof Map) {
                        return (Map<String, Object>) data;
                    }
                    return Map.<String, Object>of();
                })
                .onErrorReturn(Map.of());
    }

    private Mono<Map<String, Object>> fetchCustomerStats(String token) {
        return customerServiceClient.get()
                .uri("/api/customers/stats")
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    Object data = response.get("data");
                    if (data instanceof Map) {
                        return (Map<String, Object>) data;
                    }
                    return Map.<String, Object>of();
                })
                .onErrorReturn(Map.of());
    }

    private Mono<OrderStats> fetchOrderStatsForBroker(String brokerId, String token) {
        return orderServiceClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/orders/stats")
                        .queryParam("brokerId", brokerId)
                        .build())
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(Map.class)
                .map(this::mapToOrderStats)
                .onErrorReturn(createEmptyOrderStats());
    }

    private Mono<OrderStats> fetchOrderStatsForCustomer(String customerId, String token) {
        return orderServiceClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/orders/stats")
                        .queryParam("customerId", customerId)
                        .build())
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(Map.class)
                .map(this::mapToOrderStats)
                .onErrorReturn(createEmptyOrderStats());
    }

    private Mono<AssetStats> fetchAdminAssetStats(String token) {
        return assetServiceClient.get()
                .uri("/api/assets/stats")
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(Map.class)
                .map(this::mapToAssetStats)
                .onErrorReturn(createEmptyAssetStats());
    }

    private Mono<AssetStats> fetchBrokerAssetStats(String brokerId, String token) {
        return assetServiceClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/assets/stats")
                        .queryParam("brokerId", brokerId)
                        .build())
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(Map.class)
                .map(this::mapToAssetStats)
                .onErrorReturn(createEmptyAssetStats());
    }

    private Mono<AssetStats> fetchCustomerAssetStats(String customerId, String token) {
        return assetServiceClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/assets")
                        .queryParam("customerId", customerId)
                        .build())
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(List.class)
                .map(this::mapAssetsToStats)
                .onErrorReturn(createEmptyAssetStats());
    }

    private Mono<SystemHealth> fetchSystemHealth(String token) {
        return Mono.zip(
                checkServiceHealth(orderServiceClient),
                checkServiceHealth(assetServiceClient),
                checkServiceHealth(customerServiceClient)
        ).map(tuple -> SystemHealth.builder()
                .orderService(tuple.getT1())
                .assetService(tuple.getT2())
                .customerService(tuple.getT3())
                .notificationService("UP")
                .auditService("UP")
                .kafkaStatus("UP")
                .redisStatus("UP")
                .build());
    }

    private Mono<String> checkServiceHealth(WebClient client) {
        return client.get()
                .uri("/actuator/health")
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> "UP")
                .onErrorReturn("DOWN");
    }

    @SuppressWarnings("unchecked")
    private OrderStats mapToOrderStats(Map<String, Object> data) {
        return OrderStats.builder()
                .pendingOrders(getLong(data, "pendingOrders"))
                .matchedOrders(getLong(data, "matchedOrders"))
                .cancelledOrders(getLong(data, "cancelledOrders"))
                .totalOrders(getLong(data, "totalOrders"))
                .totalVolume(getBigDecimal(data, "totalVolume"))
                .build();
    }

    @SuppressWarnings("unchecked")
    private List<RecentOrder> mapToRecentOrders(Map<String, Object> data) {
        Object recentOrdersObj = data.get("recentOrders");
        if (recentOrdersObj instanceof List<?> recentList) {
            return recentList.stream()
                    .filter(o -> o instanceof Map)
                    .map(o -> (Map<String, Object>) o)
                    .map(order -> RecentOrder.builder()
                            .orderId((String) order.get("id"))
                            .customerId((String) order.get("customerId"))
                            .assetName((String) order.get("assetName"))
                            .side((String) order.get("orderSide"))
                            .size(getBigDecimal(order, "size"))
                            .price(getBigDecimal(order, "price"))
                            .status((String) order.get("status"))
                            .build())
                    .limit(10)
                    .toList();
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private AssetStats mapToAssetStats(Map<String, Object> data) {
        return AssetStats.builder()
                .totalAssets(getInt(data, "totalAssets"))
                .tryBalance(getBigDecimal(data, "tryBalance"))
                .tryUsable(getBigDecimal(data, "tryUsable"))
                .tryBlocked(getBigDecimal(data, "tryBlocked"))
                .portfolioValue(getBigDecimal(data, "portfolioValue"))
                .build();
    }

    @SuppressWarnings("unchecked")
    private AssetStats mapAssetsToStats(List<Map<String, Object>> assets) {
        BigDecimal tryBalance = BigDecimal.ZERO;
        BigDecimal tryUsable = BigDecimal.ZERO;

        for (Map<String, Object> asset : assets) {
            String assetName = (String) asset.get("assetName");
            if ("TRY".equals(assetName)) {
                tryBalance = getBigDecimal(asset, "size");
                tryUsable = getBigDecimal(asset, "usableSize");
            }
        }

        return AssetStats.builder()
                .totalAssets(assets.size())
                .tryBalance(tryBalance)
                .tryUsable(tryUsable)
                .tryBlocked(tryBalance.subtract(tryUsable))
                .portfolioValue(tryBalance)
                .build();
    }

    private String extractPrimaryRole(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .filter(role -> List.of("ADMIN", "BROKER", "CUSTOMER").contains(role))
                .findFirst()
                .orElse("CUSTOMER");
    }

    private DashboardDTO createEmptyDashboard(String role, String userId, String username) {
        return DashboardDTO.builder()
                .role(role)
                .userId(userId)
                .username(username)
                .timestamp(LocalDateTime.now())
                .orderStats(createEmptyOrderStats())
                .assetStats(createEmptyAssetStats())
                .build();
    }

    private OrderStats createEmptyOrderStats() {
        return OrderStats.builder()
                .pendingOrders(0L)
                .matchedOrders(0L)
                .cancelledOrders(0L)
                .totalOrders(0L)
                .totalVolume(BigDecimal.ZERO)
                .build();
    }

    private AssetStats createEmptyAssetStats() {
        return AssetStats.builder()
                .totalAssets(0)
                .tryBalance(BigDecimal.ZERO)
                .tryUsable(BigDecimal.ZERO)
                .tryBlocked(BigDecimal.ZERO)
                .portfolioValue(BigDecimal.ZERO)
                .build();
    }

    private long getLong(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return 0L;
    }

    private int getInt(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return 0;
    }

    private BigDecimal getBigDecimal(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        return BigDecimal.ZERO;
    }
}
