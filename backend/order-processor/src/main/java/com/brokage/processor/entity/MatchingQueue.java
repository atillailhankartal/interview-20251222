package com.brokage.processor.entity;

import com.brokage.common.entity.BaseEntity;
import com.brokage.common.enums.OrderSide;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "matching_queue", indexes = {
        @Index(name = "idx_queue_asset_side", columnList = "asset_name, order_side"),
        @Index(name = "idx_queue_priority", columnList = "tier_priority, price, queued_at"),
        @Index(name = "idx_queue_order", columnList = "order_id"),
        @Index(name = "idx_queue_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchingQueue extends BaseEntity {

    @Column(name = "order_id", nullable = false, unique = true)
    private UUID orderId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "asset_name", nullable = false, length = 20)
    private String assetName;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_side", nullable = false)
    private OrderSide orderSide;

    @Column(name = "price", nullable = false, precision = 19, scale = 4)
    private BigDecimal price;

    @Column(name = "remaining_size", nullable = false, precision = 19, scale = 4)
    private BigDecimal remainingSize;

    @Column(name = "tier_priority", nullable = false)
    private Integer tierPriority;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private QueueStatus status = QueueStatus.ACTIVE;

    @Column(name = "queued_at", nullable = false)
    @Builder.Default
    private LocalDateTime queuedAt = LocalDateTime.now();

    @Column(name = "matched_at")
    private LocalDateTime matchedAt;

    @Column(name = "removed_at")
    private LocalDateTime removedAt;

    @Column(name = "remove_reason", length = 255)
    private String removeReason;

    public enum QueueStatus {
        ACTIVE,
        PARTIALLY_MATCHED,
        FULLY_MATCHED,
        CANCELED,
        EXPIRED
    }

    public boolean isActive() {
        return status == QueueStatus.ACTIVE || status == QueueStatus.PARTIALLY_MATCHED;
    }

    public void partialMatch(BigDecimal matchedSize) {
        this.remainingSize = this.remainingSize.subtract(matchedSize);
        if (this.remainingSize.compareTo(BigDecimal.ZERO) <= 0) {
            this.status = QueueStatus.FULLY_MATCHED;
            this.matchedAt = LocalDateTime.now();
        } else {
            this.status = QueueStatus.PARTIALLY_MATCHED;
        }
    }

    public void cancel(String reason) {
        this.status = QueueStatus.CANCELED;
        this.removedAt = LocalDateTime.now();
        this.removeReason = reason;
    }
}
