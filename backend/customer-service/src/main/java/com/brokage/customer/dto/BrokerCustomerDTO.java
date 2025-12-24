package com.brokage.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrokerCustomerDTO {

    private UUID id;
    private UUID brokerId;
    private UUID customerId;
    private String customerName;
    private String customerEmail;
    private Boolean active;
    private String notes;
    private LocalDateTime createdAt;
}
