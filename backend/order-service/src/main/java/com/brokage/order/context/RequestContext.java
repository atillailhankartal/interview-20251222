package com.brokage.order.context;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

/**
 * Holds request context information for audit enrichment.
 */
@Data
@Builder
public class RequestContext {
    private String traceId;
    private String spanId;
    private String ipAddress;
    private String userAgent;
    private String requestId;
    private UUID performedBy;
    private String performedByRole;

    private static final ThreadLocal<RequestContext> CONTEXT = new ThreadLocal<>();

    public static void set(RequestContext context) {
        CONTEXT.set(context);
    }

    public static RequestContext get() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
