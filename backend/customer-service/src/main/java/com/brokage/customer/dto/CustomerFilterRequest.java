package com.brokage.customer.dto;

import com.brokage.common.enums.CustomerRole;
import com.brokage.common.enums.CustomerStatus;
import com.brokage.common.enums.CustomerTier;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerFilterRequest {

    private CustomerTier tier;
    private CustomerStatus status;
    private CustomerRole role;
    private String search;

    /**
     * If true, only return customers that can have orders (role = CUSTOMER)
     */
    @Builder.Default
    private boolean orderableOnly = false;

    @Builder.Default
    private int page = 0;

    @Builder.Default
    private int size = 20;
}
