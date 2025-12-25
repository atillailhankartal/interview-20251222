package com.brokage.asset.entity;

import com.brokage.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "customer_assets",
       uniqueConstraints = @UniqueConstraint(columnNames = {"customer_id", "asset_name"}),
       indexes = {
        @Index(name = "idx_customer_assets_customer", columnList = "customer_id"),
        @Index(name = "idx_customer_assets_name", columnList = "asset_name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerAsset extends BaseEntity {

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "asset_name", nullable = false, length = 20)
    private String assetName;

    @Column(name = "size", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal size = BigDecimal.ZERO;

    @Column(name = "usable_size", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal usableSize = BigDecimal.ZERO;

    /**
     * Blocked size is calculated: size - usableSize
     * This represents the amount reserved for pending orders
     */
    public BigDecimal getBlockedSize() {
        return size.subtract(usableSize);
    }

    public boolean hasAvailableBalance(BigDecimal amount) {
        return usableSize.compareTo(amount) >= 0;
    }

    /**
     * Block amount for pending order
     * Only usableSize decreases, size stays the same
     * Blocked amount (size - usableSize) increases implicitly
     */
    public void blockAmount(BigDecimal amount) {
        if (!hasAvailableBalance(amount)) {
            throw new IllegalStateException("Insufficient usable balance");
        }
        this.usableSize = this.usableSize.subtract(amount);
    }

    /**
     * Release blocked amount when order is cancelled
     * Only usableSize increases, size stays the same
     * Blocked amount decreases implicitly
     */
    public void releaseBlockedAmount(BigDecimal amount) {
        BigDecimal blocked = getBlockedSize();
        if (blocked.compareTo(amount) < 0) {
            throw new IllegalStateException("Cannot release more than blocked amount");
        }
        this.usableSize = this.usableSize.add(amount);
    }

    /**
     * Deduct blocked amount when order is matched (settlement)
     * Size decreases, usableSize stays the same
     * Blocked amount decreases implicitly
     */
    public void deductBlockedAmount(BigDecimal amount) {
        BigDecimal blocked = getBlockedSize();
        if (blocked.compareTo(amount) < 0) {
            throw new IllegalStateException("Cannot deduct more than blocked amount");
        }
        this.size = this.size.subtract(amount);
    }

    /**
     * Add to both size and usableSize when receiving assets from match
     * Both size and usableSize increase (asset is immediately available)
     */
    public void addUsableAmount(BigDecimal amount) {
        this.size = this.size.add(amount);
        this.usableSize = this.usableSize.add(amount);
    }
}
