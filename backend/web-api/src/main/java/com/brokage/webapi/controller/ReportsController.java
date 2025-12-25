package com.brokage.webapi.controller;

import com.brokage.webapi.dto.ReportDTO;
import com.brokage.webapi.service.ReportsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;

/**
 * Reports Controller - Role-based reporting
 *
 * Access Control:
 * - CUSTOMER: Only their own portfolio and transaction reports
 * - BROKER: Assigned customers' reports + broker performance reports
 * - ADMIN: All reports including system-wide reports
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Slf4j
public class ReportsController {

    private final ReportsService reportsService;

    /**
     * Get daily trading summary - ADMIN and BROKER only
     * BROKER gets only their customers' trading summary
     */
    @GetMapping("/trading/daily")
    @PreAuthorize("hasAnyRole('ADMIN', 'BROKER')")
    public Mono<ResponseEntity<ReportDTO>> getDailyTradingSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Authentication authentication) {

        LocalDate reportDate = date != null ? date : LocalDate.now();
        String token = extractToken(authentication);
        String role = extractPrimaryRole(authentication);

        log.info("{} {} requesting daily trading summary for {}",
                role, extractUsername(authentication), reportDate);

        return reportsService.generateDailyTradingSummary(reportDate, token)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("Error generating daily trading summary", e);
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }

    /**
     * Get customer portfolio report
     * - CUSTOMER: Only their own portfolio
     * - BROKER: Only assigned customers' portfolios
     * - ADMIN: Any customer's portfolio
     */
    @GetMapping("/portfolio")
    public Mono<ResponseEntity<ReportDTO>> getPortfolioReport(
            @RequestParam(required = false) String customerId,
            Authentication authentication) {

        String token = extractToken(authentication);
        String role = extractPrimaryRole(authentication);
        String userId = extractUserId(authentication);

        // If customerId not provided, use current user's ID
        String targetCustomerId = customerId != null ? customerId : userId;

        // Security check: CUSTOMER can only view their own portfolio
        if ("CUSTOMER".equals(role) && !userId.equals(targetCustomerId)) {
            log.warn("Customer {} attempted to access portfolio of {}", userId, targetCustomerId);
            return Mono.just(ResponseEntity.status(403).build());
        }

        log.info("{} {} requesting portfolio report for customer {}",
                role, extractUsername(authentication), targetCustomerId);

        return reportsService.generateCustomerPortfolioReport(targetCustomerId, token)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("Error generating portfolio report", e);
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }

    /**
     * Get transaction history report
     * - CUSTOMER: Only their own transactions
     * - BROKER: Only assigned customers' transactions
     * - ADMIN: Any customer's transactions
     */
    @GetMapping("/transactions")
    public Mono<ResponseEntity<ReportDTO>> getTransactionHistory(
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Authentication authentication) {

        String token = extractToken(authentication);
        String role = extractPrimaryRole(authentication);
        String userId = extractUserId(authentication);

        // If customerId not provided, use current user's ID
        String targetCustomerId = customerId != null ? customerId : userId;

        // Security check: CUSTOMER can only view their own transactions
        if ("CUSTOMER".equals(role) && !userId.equals(targetCustomerId)) {
            log.warn("Customer {} attempted to access transactions of {}", userId, targetCustomerId);
            return Mono.just(ResponseEntity.status(403).build());
        }

        LocalDate start = startDate != null ? startDate : LocalDate.now().minusMonths(1);
        LocalDate end = endDate != null ? endDate : LocalDate.now();

        log.info("{} {} requesting transaction history for customer {} ({} to {})",
                role, extractUsername(authentication), targetCustomerId, start, end);

        return reportsService.generateTransactionHistory(targetCustomerId, start, end, token)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("Error generating transaction history", e);
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }

    /**
     * Get broker performance report - ADMIN only
     */
    @GetMapping("/broker-performance/{brokerId}")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<ReportDTO>> getBrokerPerformanceReport(
            @PathVariable String brokerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Authentication authentication) {

        String token = extractToken(authentication);

        LocalDate start = startDate != null ? startDate : LocalDate.now().minusMonths(1);
        LocalDate end = endDate != null ? endDate : LocalDate.now();

        log.info("Admin {} requesting broker performance report for {} ({} to {})",
                extractUsername(authentication), brokerId, start, end);

        return reportsService.generateBrokerPerformanceReport(brokerId, start, end, token)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("Error generating broker performance report", e);
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }

    /**
     * Get my performance report - BROKER only (their own stats)
     */
    @GetMapping("/my-performance")
    @PreAuthorize("hasRole('BROKER')")
    public Mono<ResponseEntity<ReportDTO>> getMyPerformanceReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Authentication authentication) {

        String token = extractToken(authentication);
        String brokerId = extractUserId(authentication);

        LocalDate start = startDate != null ? startDate : LocalDate.now().minusMonths(1);
        LocalDate end = endDate != null ? endDate : LocalDate.now();

        log.info("Broker {} requesting their own performance report ({} to {})",
                extractUsername(authentication), start, end);

        return reportsService.generateBrokerPerformanceReport(brokerId, start, end, token)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("Error generating broker performance report", e);
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }

    private String extractToken(Authentication authentication) {
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt.getTokenValue();
        }
        return "";
    }

    private String extractUserId(Authentication authentication) {
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt.getSubject();
        }
        return "unknown";
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
