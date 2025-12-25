package com.brokage.order.config;

import com.brokage.order.context.RequestContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Filter that captures request context for audit enrichment.
 */
@Component("auditRequestContextFilter")
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@Slf4j
public class RequestContextFilter extends OncePerRequestFilter {

    private static final String TRACE_ID_HEADER = "X-B3-TraceId";
    private static final String SPAN_ID_HEADER = "X-B3-SpanId";
    private static final String REQUEST_ID_HEADER = "X-Request-ID";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            RequestContext context = buildContext(request);
            RequestContext.set(context);
            filterChain.doFilter(request, response);
        } finally {
            RequestContext.clear();
        }
    }

    private RequestContext buildContext(HttpServletRequest request) {
        RequestContext.RequestContextBuilder builder = RequestContext.builder()
                .traceId(getHeader(request, TRACE_ID_HEADER))
                .spanId(getHeader(request, SPAN_ID_HEADER))
                .requestId(getHeader(request, REQUEST_ID_HEADER))
                .ipAddress(getClientIpAddress(request))
                .userAgent(request.getHeader("User-Agent"));

        // Try to get user info from security context
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            String email = jwt.getClaimAsString("email");
            String role = extractRole(jwt);

            // Use subject as performedBy for now (will be resolved by customer lookup)
            try {
                builder.performedBy(UUID.fromString(jwt.getSubject()));
            } catch (IllegalArgumentException e) {
                // Subject is not a UUID, skip
            }
            builder.performedByRole(role);
        }

        return builder.build();
    }

    private String getHeader(HttpServletRequest request, String headerName) {
        String value = request.getHeader(headerName);
        return value != null && !value.isEmpty() ? value : null;
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String[] headerNames = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP"
        };

        for (String header : headerNames) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // X-Forwarded-For can contain multiple IPs, take the first one
                return ip.split(",")[0].trim();
            }
        }

        return request.getRemoteAddr();
    }

    private String extractRole(Jwt jwt) {
        var realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess != null) {
            var roles = realmAccess.get("roles");
            if (roles instanceof Collection<?> roleList) {
                for (Object role : roleList) {
                    String roleStr = role.toString().toUpperCase();
                    if (roleStr.equals("ADMIN") || roleStr.equals("BROKER") || roleStr.equals("CUSTOMER")) {
                        return roleStr;
                    }
                }
            }
        }
        return null;
    }
}
