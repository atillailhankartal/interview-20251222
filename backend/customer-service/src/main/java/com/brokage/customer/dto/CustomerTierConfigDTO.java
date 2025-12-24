package com.brokage.customer.dto;

import com.brokage.common.enums.CustomerTier;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerTierConfigDTO {

    private UUID id;
    private CustomerTier tier;
    private String displayName;
    private Integer priority;
    private Integer rateLimitPerMinute;
    private Integer rateLimitPerDay;
    private BigDecimal monthlyFee;
    private BigDecimal maxOrderValue;
    private String description;
    private Boolean active;
}
