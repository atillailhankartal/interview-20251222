package com.brokage.order.config;

import com.brokage.common.enums.CustomerTier;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Tier-based rate limiting filter.
 *
 * Rate limits per tier (requests per minute):
 * - VIP: 1000 req/min
 * - PREMIUM: 500 req/min
 * - STANDARD: 100 req/min
 *
 * Note: This is a simple in-memory implementation.
 * For production, use Redis-backed rate limiting (e.g., Bucket4j with Redis).
 */
@Component
@Slf4j
public class TierRateLimitFilter extends OncePerRequestFilter {

    private static final Map<CustomerTier, Integer> TIER_LIMITS = Map.of(
            CustomerTier.VIP, 1000,
            CustomerTier.PREMIUM, 500,
            CustomerTier.STANDARD, 100
    );

    private static final long WINDOW_SIZE_MS = 60_000; // 1 minute

    // Simple in-memory rate limiting (userId -> counter)
    // For production: use Redis with sliding window algorithm
    private final ConcurrentHashMap<String, RateLimitBucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }

        String userId = extractUserId(auth);
        CustomerTier tier = extractTier(auth);

        if (userId == null) {
            filterChain.doFilter(request, response);
            return;
        }

        int limit = TIER_LIMITS.getOrDefault(tier, TIER_LIMITS.get(CustomerTier.STANDARD));

        RateLimitBucket bucket = buckets.computeIfAbsent(userId,
                k -> new RateLimitBucket(limit, WINDOW_SIZE_MS));

        if (bucket.tryAcquire()) {
            // Add rate limit headers
            response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
            response.setHeader("X-RateLimit-Remaining", String.valueOf(bucket.getRemaining()));
            response.setHeader("X-RateLimit-Reset", String.valueOf(bucket.getResetTime()));
            response.setHeader("X-Customer-Tier", tier.name());

            filterChain.doFilter(request, response);
        } else {
            log.warn("Rate limit exceeded for user: {}, tier: {}", userId, tier);

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
            response.setHeader("X-RateLimit-Remaining", "0");
            response.setHeader("X-RateLimit-Reset", String.valueOf(bucket.getResetTime()));
            response.setHeader("Retry-After", String.valueOf((bucket.getResetTime() - System.currentTimeMillis()) / 1000));
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded for " + tier + " tier. Limit: " + limit + " requests/minute\"}"
            );
        }
    }

    private String extractUserId(Authentication auth) {
        if (auth.getPrincipal() instanceof Jwt jwt) {
            return jwt.getSubject();
        }
        return auth.getName();
    }

    private CustomerTier extractTier(Authentication auth) {
        if (auth.getPrincipal() instanceof Jwt jwt) {
            // Try to get tier from JWT claims
            String tierClaim = jwt.getClaimAsString("customer_tier");
            if (tierClaim != null) {
                try {
                    return CustomerTier.valueOf(tierClaim.toUpperCase());
                } catch (IllegalArgumentException e) {
                    log.debug("Unknown tier claim: {}", tierClaim);
                }
            }
        }
        // Default to STANDARD tier
        return CustomerTier.STANDARD;
    }

    /**
     * Simple rate limit bucket with sliding window.
     */
    private static class RateLimitBucket {
        private final int limit;
        private final long windowSizeMs;
        private final AtomicInteger count = new AtomicInteger(0);
        private final AtomicLong windowStart = new AtomicLong(System.currentTimeMillis());

        RateLimitBucket(int limit, long windowSizeMs) {
            this.limit = limit;
            this.windowSizeMs = windowSizeMs;
        }

        synchronized boolean tryAcquire() {
            long now = System.currentTimeMillis();
            long start = windowStart.get();

            // Check if window has expired
            if (now - start >= windowSizeMs) {
                // Reset window
                windowStart.set(now);
                count.set(1);
                return true;
            }

            // Check if under limit
            if (count.get() < limit) {
                count.incrementAndGet();
                return true;
            }

            return false;
        }

        int getRemaining() {
            return Math.max(0, limit - count.get());
        }

        long getResetTime() {
            return windowStart.get() + windowSizeMs;
        }
    }
}
