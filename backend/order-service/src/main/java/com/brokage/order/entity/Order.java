package com.brokage.order.entity;

import com.brokage.common.entity.BaseEntity;
import com.brokage.common.enums.OrderSide;
import com.brokage.common.enums.OrderStatus;
import com.brokage.common.enums.OrderType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "orders", indexes = {
        @Index(name = "idx_order_customer", columnList = "customer_id"),
        @Index(name = "idx_order_asset", columnList = "asset_symbol"),
        @Index(name = "idx_order_status", columnList = "status"),
        @Index(name = "idx_order_side", columnList = "order_side"),
        @Index(name = "idx_order_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order extends BaseEntity {

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "asset_symbol", nullable = false, length = 20)
    private String assetSymbol;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_side", nullable = false)
    private OrderSide orderSide;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", nullable = false)
    @Builder.Default
    private OrderType orderType = OrderType.LIMIT;

    @Column(name = "size", nullable = false, precision = 19, scale = 4)
    private BigDecimal size;

    @Column(name = "price", nullable = false, precision = 19, scale = 4)
    private BigDecimal price;

    @Column(name = "filled_size", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal filledSize = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    @Column(name = "customer_tier_priority")
    private Integer customerTierPriority;

    @Column(name = "idempotency_key", unique = true, length = 64)
    private String idempotencyKey;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "matched_at")
    private java.time.LocalDateTime matchedAt;

    @Column(name = "cancelled_at")
    private java.time.LocalDateTime cancelledAt;

    public BigDecimal getRemainingSize() {
        return size.subtract(filledSize);
    }

    public BigDecimal getTotalValue() {
        return size.multiply(price);
    }

    public BigDecimal getFilledValue() {
        return filledSize.multiply(price);
    }

    public boolean isFullyFilled() {
        return filledSize.compareTo(size) >= 0;
    }

    public boolean isPartiallyFilled() {
        return filledSize.compareTo(BigDecimal.ZERO) > 0 && !isFullyFilled();
    }

    public boolean isCancellable() {
        return status == OrderStatus.PENDING || status == OrderStatus.PARTIALLY_FILLED;
    }
}
