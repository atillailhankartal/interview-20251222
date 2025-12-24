package com.brokage.asset.entity;

import com.brokage.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "asset_reservations", indexes = {
        @Index(name = "idx_reservation_customer", columnList = "customer_id"),
        @Index(name = "idx_reservation_order", columnList = "order_id"),
        @Index(name = "idx_reservation_asset", columnList = "asset_symbol"),
        @Index(name = "idx_reservation_status", columnList = "status"),
        @Index(name = "idx_reservation_expires", columnList = "expires_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetReservation extends BaseEntity {

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "asset_symbol", nullable = false, length = 20)
    private String assetSymbol;

    @Column(name = "reserved_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal reservedAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private ReservationStatus status = ReservationStatus.ACTIVE;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "released_at")
    private LocalDateTime releasedAt;

    @Column(name = "release_reason", length = 255)
    private String releaseReason;

    public enum ReservationStatus {
        ACTIVE,
        CONSUMED,
        RELEASED,
        EXPIRED
    }

    public boolean isActive() {
        return status == ReservationStatus.ACTIVE;
    }

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public void consume() {
        this.status = ReservationStatus.CONSUMED;
        this.releasedAt = LocalDateTime.now();
    }

    public void release(String reason) {
        this.status = ReservationStatus.RELEASED;
        this.releasedAt = LocalDateTime.now();
        this.releaseReason = reason;
    }

    public void expire() {
        this.status = ReservationStatus.EXPIRED;
        this.releasedAt = LocalDateTime.now();
        this.releaseReason = "Reservation expired";
    }
}
