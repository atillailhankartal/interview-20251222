package com.brokage.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerStatsDTO {
    private long totalCustomers;
    private long totalBrokers;
    private long totalAdmins;
    private long activeUsers;
    private long inactiveUsers;
}
