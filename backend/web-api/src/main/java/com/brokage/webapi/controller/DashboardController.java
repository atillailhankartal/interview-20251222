package com.brokage.webapi.controller;

import com.brokage.webapi.dto.DashboardDTO;
import com.brokage.webapi.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * Get dashboard data based on user's role
     * - ADMIN: System-wide statistics, all customers, all orders
     * - BROKER: Assigned customers, their orders, portfolio summaries
     * - CUSTOMER: Own orders, own assets, PnL
     */
    @GetMapping
    public Mono<ResponseEntity<DashboardDTO>> getDashboard(Authentication authentication) {
        String token = extractToken(authentication);
        return dashboardService.getDashboard(authentication, token)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("Error fetching dashboard", e);
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }

    /**
     * Refresh dashboard data (force fetch from services)
     */
    @PostMapping("/refresh")
    public Mono<ResponseEntity<DashboardDTO>> refreshDashboard(Authentication authentication) {
        String token = extractToken(authentication);
        return dashboardService.getDashboard(authentication, token)
                .map(ResponseEntity::ok);
    }

    private String extractToken(Authentication authentication) {
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt.getTokenValue();
        }
        return "";
    }
}
