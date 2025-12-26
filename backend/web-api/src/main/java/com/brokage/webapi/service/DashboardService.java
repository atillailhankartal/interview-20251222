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
        String keycloakUserId = jwt.getSubject();
        String username = jwt.getClaimAsString("preferred_username");
        String email = jwt.getClaimAsString("email");
        String role = extractPrimaryRole(authentication);

        log.info("Building dashboard for user: {} with role: {} and email: {}", username, role, email);

        return switch (role) {
            case "ADMIN" -> buildAdminDashboard(keycloakUserId, username, token);
            case "BROKER" -> resolveUserIdByToken(token)
                    .flatMap(brokerId -> buildBrokerDashboard(brokerId, username, token))
                    .switchIfEmpty(Mono.defer(() -> {
                        log.warn("Could not resolve broker ID for email: {}", email);
                        return Mono.just(createEmptyDashboard("BROKER", keycloakUserId, username));
                    }));
            case "CUSTOMER" -> resolveUserIdByToken(token)
                    .flatMap(customerId -> buildCustomerDashboard(customerId, username, token))
                    .switchIfEmpty(Mono.defer(() -> buildCustomerDashboard(keycloakUserId, username, token)));
            default -> buildCustomerDashboard(keycloakUserId, username, token);
        };
    }

    /**
     * Resolve broker/customer ID by calling /api/customers/me endpoint
     * This endpoint auto-links Keycloak users to database customers by email
     */
    @SuppressWarnings("unchecked")
    private Mono<String> resolveUserIdByToken(String token) {
        return customerServiceClient.get()
                .uri("/api/customers/me")
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    Object dataObj = response.get("data");
                    if (dataObj instanceof Map) {
                        Map<String, Object> customer = (Map<String, Object>) dataObj;
                        return (String) customer.get("id");
                    }
                    return null;
                })
                .filter(id -> id != null)
                .doOnNext(id -> log.info("Resolved user to ID: {}", id))
                .onErrorResume(e -> {
                    log.error("Error resolving user ID from /api/customers/me", e);
                    return Mono.empty();
                });
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

            List<TopTrader> topTraders = mapToTopTraders(orderData);

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
                            .topTraders(topTraders)
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
                fetchBrokerAssetStats(userId, token),
                fetchBrokerCustomers(userId, token),
                fetchBrokerCustomerOrders(userId, token)
        ).map(tuple -> {
            OrderStats orderStats = tuple.getT1();
            AssetStats assetStats = tuple.getT2();
            BrokerCustomerData customerData = tuple.getT3();
            List<RecentOrder> customerOrders = tuple.getT4();

            return DashboardDTO.builder()
                    .role("BROKER")
                    .userId(userId)
                    .username(username)
                    .timestamp(LocalDateTime.now())
                    .orderStats(orderStats)
                    .assetStats(assetStats)
                    .brokerData(BrokerDashboard.builder()
                            .assignedCustomers(customerData.assignedCustomers())
                            .activeCustomers(customerData.activeCustomers())
                            .customersPortfolioValue(customerData.portfolioValue())
                            .customerList(customerData.customers())
                            .customerOrders(customerOrders)
                            .build())
                    .build();
        }).onErrorResume(e -> {
            log.error("Error building broker dashboard", e);
            return Mono.just(createEmptyDashboard("BROKER", userId, username));
        });
    }

    private record BrokerCustomerData(long assignedCustomers, long activeCustomers, BigDecimal portfolioValue, List<CustomerSummary> customers) {}

    @SuppressWarnings("unchecked")
    private Mono<BrokerCustomerData> fetchBrokerCustomers(String brokerId, String token) {
        return customerServiceClient.get()
                .uri("/api/customers/broker/" + brokerId + "/customers")
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    Object dataObj = response.get("data");
                    List<Map<String, Object>> customers = List.of();
                    if (dataObj instanceof Map) {
                        Map<String, Object> pageData = (Map<String, Object>) dataObj;
                        Object content = pageData.get("content");
                        if (content instanceof List) {
                            customers = (List<Map<String, Object>>) content;
                        }
                    } else if (dataObj instanceof List) {
                        customers = (List<Map<String, Object>>) dataObj;
                    }

                    List<CustomerSummary> customerList = customers.stream()
                            .limit(10)
                            .map(c -> CustomerSummary.builder()
                                    .customerId((String) c.get("id"))
                                    .name(c.get("firstName") + " " + c.get("lastName"))
                                    .email((String) c.get("email"))
                                    .orderCount(0L)
                                    .portfolioValue(BigDecimal.ZERO)
                                    .build())
                            .toList();

                    return new BrokerCustomerData(
                            customers.size(),
                            customers.stream().filter(c -> "ACTIVE".equals(c.get("status"))).count(),
                            BigDecimal.ZERO,
                            customerList
                    );
                })
                .onErrorReturn(new BrokerCustomerData(0, 0, BigDecimal.ZERO, List.of()));
    }

    @SuppressWarnings("unchecked")
    private Mono<List<RecentOrder>> fetchBrokerCustomerOrders(String brokerId, String token) {
        return orderServiceClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/orders")
                        .queryParam("brokerId", brokerId)
                        .queryParam("size", 10)
                        .build())
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    Object dataObj = response.get("data");
                    List<Map<String, Object>> orders = List.of();
                    if (dataObj instanceof Map) {
                        Map<String, Object> pageData = (Map<String, Object>) dataObj;
                        Object content = pageData.get("content");
                        if (content instanceof List) {
                            orders = (List<Map<String, Object>>) content;
                        }
                    } else if (dataObj instanceof List) {
                        orders = (List<Map<String, Object>>) dataObj;
                    }

                    return orders.stream()
                            .map(o -> RecentOrder.builder()
                                    .id((String) o.get("id"))
                                    .customerId((String) o.get("customerId"))
                                    .assetName((String) o.get("assetSymbol"))
                                    .orderSide((String) o.get("orderSide"))
                                    .size(getBigDecimal(o, "size"))
                                    .price(getBigDecimal(o, "price"))
                                    .status((String) o.get("status"))
                                    .createdAt(parseDateTime(o.get("createdAt")))
                                    .build())
                            .limit(10)
                            .toList();
                })
                .onErrorReturn(List.of());
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
                            .id((String) order.get("id"))
                            .customerId((String) order.get("customerId"))
                            .assetName((String) order.get("assetName"))
                            .orderSide((String) order.get("orderSide"))
                            .size(getBigDecimal(order, "size"))
                            .price(getBigDecimal(order, "price"))
                            .status((String) order.get("status"))
                            .createdAt(parseDateTime(order.get("createdAt")))
                            .build())
                    .limit(10)
                    .toList();
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private List<TopTrader> mapToTopTraders(Map<String, Object> data) {
        Object topTradersObj = data.get("topTraders");
        if (topTradersObj instanceof List<?> traderList) {
            return traderList.stream()
                    .filter(o -> o instanceof Map)
                    .map(o -> (Map<String, Object>) o)
                    .map(trader -> TopTrader.builder()
                            .customerId((String) trader.get("customerId"))
                            .customerName((String) trader.get("customerName"))
                            .orderCount(getLong(trader, "orderCount"))
                            .tradingVolume(getBigDecimal(trader, "tradingVolume"))
                            .build())
                    .limit(5)
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

    private LocalDateTime parseDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String dateStr) {
            try {
                return LocalDateTime.parse(dateStr.replace(" ", "T"));
            } catch (Exception e) {
                log.debug("Failed to parse date: {}", dateStr);
                return null;
            }
        }
        return null;
    }
}
