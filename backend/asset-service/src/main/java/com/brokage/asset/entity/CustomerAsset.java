package com.brokage.asset.entity;

import com.brokage.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "customer_assets",
       uniqueConstraints = @UniqueConstraint(columnNames = {"customer_id", "asset_symbol"}),
       indexes = {
        @Index(name = "idx_customer_assets_customer", columnList = "customer_id"),
        @Index(name = "idx_customer_assets_symbol", columnList = "asset_symbol")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerAsset extends BaseEntity {

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "asset_symbol", nullable = false, length = 20)
    private String assetSymbol;

    @Column(name = "usable_size", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal usableSize = BigDecimal.ZERO;

    @Column(name = "blocked_size", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal blockedSize = BigDecimal.ZERO;

    public BigDecimal getTotalSize() {
        return usableSize.add(blockedSize);
    }

    public boolean hasAvailableBalance(BigDecimal amount) {
        return usableSize.compareTo(amount) >= 0;
    }

    public void blockAmount(BigDecimal amount) {
        if (!hasAvailableBalance(amount)) {
            throw new IllegalStateException("Insufficient usable balance");
        }
        this.usableSize = this.usableSize.subtract(amount);
        this.blockedSize = this.blockedSize.add(amount);
    }

    public void releaseBlockedAmount(BigDecimal amount) {
        if (blockedSize.compareTo(amount) < 0) {
            throw new IllegalStateException("Cannot release more than blocked amount");
        }
        this.blockedSize = this.blockedSize.subtract(amount);
        this.usableSize = this.usableSize.add(amount);
    }

    public void deductBlockedAmount(BigDecimal amount) {
        if (blockedSize.compareTo(amount) < 0) {
            throw new IllegalStateException("Cannot deduct more than blocked amount");
        }
        this.blockedSize = this.blockedSize.subtract(amount);
    }

    public void addUsableAmount(BigDecimal amount) {
        this.usableSize = this.usableSize.add(amount);
    }
}
