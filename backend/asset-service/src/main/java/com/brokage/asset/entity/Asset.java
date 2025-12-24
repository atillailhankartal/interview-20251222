package com.brokage.asset.entity;

import com.brokage.common.entity.BaseEntity;
import com.brokage.common.enums.AssetType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "assets", indexes = {
        @Index(name = "idx_asset_symbol", columnList = "symbol", unique = true),
        @Index(name = "idx_asset_type", columnList = "type"),
        @Index(name = "idx_asset_active", columnList = "active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Asset extends BaseEntity {

    @Column(name = "symbol", nullable = false, unique = true, length = 20)
    private String symbol;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private AssetType type;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "min_order_quantity", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal minOrderQuantity = BigDecimal.ONE;

    @Column(name = "lot_size", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal lotSize = BigDecimal.ONE;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;
}
