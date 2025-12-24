package com.brokage.customer.entity;

import com.brokage.common.entity.BaseEntity;
import com.brokage.common.enums.CustomerTier;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "customer_tier_configs", indexes = {
        @Index(name = "idx_tier_config_tier", columnList = "tier", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerTierConfig extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "tier", nullable = false, unique = true)
    private CustomerTier tier;

    @Column(name = "display_name", nullable = false, length = 50)
    private String displayName;

    @Column(name = "priority", nullable = false)
    private Integer priority;

    @Column(name = "rate_limit_per_minute", nullable = false)
    private Integer rateLimitPerMinute;

    @Column(name = "rate_limit_per_day", nullable = false)
    private Integer rateLimitPerDay;

    @Column(name = "monthly_fee", precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal monthlyFee = BigDecimal.ZERO;

    @Column(name = "max_order_value", precision = 19, scale = 2)
    private BigDecimal maxOrderValue;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;
}
