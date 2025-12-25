package com.brokage.webapi.controller;

import com.brokage.webapi.dto.AnalyticsDTO;
import com.brokage.webapi.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Analytics Controller - Role-based analytics
 *
 * Access Control:
 * - CUSTOMER: Only their own trading analytics
 * - BROKER: Their assigned customers + their own broker metrics
 * - ADMIN: Full system-wide analytics
 */
@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@Slf4j
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    /**
     * Get analytics based on user's role
     * - CUSTOMER: Personal trading stats
     * - BROKER: Assigned customers' stats + broker performance
     * - ADMIN: System-wide analytics
     */
    @GetMapping
    public Mono<ResponseEntity<AnalyticsDTO>> getAnalytics(
            @RequestParam(defaultValue = "DAY") String period,
            Authentication authentication) {

        String token = extractToken(authentication);
        String role = extractPrimaryRole(authentication);
        String username = extractUsername(authentication);

        log.info("User {} (role: {}) requesting {} analytics", username, role, period);

        return analyticsService.getAnalytics(authentication, token, period)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("Error fetching analytics", e);
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }

    /**
     * Get trading analytics - scoped by role
     */
    @GetMapping("/trading")
    public Mono<ResponseEntity<AnalyticsDTO.TradingAnalytics>> getTradingAnalytics(
            @RequestParam(defaultValue = "DAY") String period,
            Authentication authentication) {

        String token = extractToken(authentication);
        String role = extractPrimaryRole(authentication);

        log.info("User {} requesting trading analytics", extractUsername(authentication));

        return analyticsService.getAnalytics(authentication, token, period)
                .map(analytics -> ResponseEntity.ok(analytics.getTradingAnalytics()))
                .onErrorResume(e -> {
                    log.error("Error fetching trading analytics", e);
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }

    /**
     * Get customer analytics - ADMIN and BROKER only
     * BROKER sees only their assigned customers
     */
    @GetMapping("/customers")
    @PreAuthorize("hasAnyRole('ADMIN', 'BROKER')")
    public Mono<ResponseEntity<AnalyticsDTO.CustomerAnalytics>> getCustomerAnalytics(
            @RequestParam(defaultValue = "DAY") String period,
            Authentication authentication) {

        String token = extractToken(authentication);
        String role = extractPrimaryRole(authentication);

        log.info("{} {} requesting customer analytics", role, extractUsername(authentication));

        return analyticsService.getAnalytics(authentication, token, period)
                .map(analytics -> ResponseEntity.ok(analytics.getCustomerAnalytics()))
                .onErrorResume(e -> {
                    log.error("Error fetching customer analytics", e);
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }

    /**
     * Get asset analytics - scoped by role
     * CUSTOMER: Their portfolio stats
     * BROKER: Assigned customers' combined AUM
     * ADMIN: System-wide AUM
     */
    @GetMapping("/assets")
    public Mono<ResponseEntity<AnalyticsDTO.AssetAnalytics>> getAssetAnalytics(
            @RequestParam(defaultValue = "DAY") String period,
            Authentication authentication) {

        String token = extractToken(authentication);
        log.info("User {} requesting asset analytics", extractUsername(authentication));

        return analyticsService.getAnalytics(authentication, token, period)
                .map(analytics -> ResponseEntity.ok(analytics.getAssetAnalytics()))
                .onErrorResume(e -> {
                    log.error("Error fetching asset analytics", e);
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }

    /**
     * Get system performance metrics - ADMIN only
     */
    @GetMapping("/performance")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<AnalyticsDTO.PerformanceMetrics>> getPerformanceMetrics(
            Authentication authentication) {

        String token = extractToken(authentication);
        log.info("Admin {} requesting performance metrics", extractUsername(authentication));

        return analyticsService.getAnalytics(authentication, token, "DAY")
                .map(analytics -> ResponseEntity.ok(analytics.getPerformanceMetrics()))
                .onErrorResume(e -> {
                    log.error("Error fetching performance metrics", e);
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }

    private String extractToken(Authentication authentication) {
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt.getTokenValue();
        }
        return "";
    }

    private String extractUsername(Authentication authentication) {
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt.getClaimAsString("preferred_username");
        }
        return "unknown";
    }

    private String extractPrimaryRole(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .filter(role -> List.of("ADMIN", "BROKER", "CUSTOMER").contains(role))
                .findFirst()
                .orElse("CUSTOMER");
    }
}
