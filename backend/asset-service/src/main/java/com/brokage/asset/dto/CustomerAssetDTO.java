package com.brokage.asset.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerAssetDTO {

    private UUID id;
    private UUID customerId;
    private String assetSymbol;
    private BigDecimal usableSize;
    private BigDecimal blockedSize;
    private BigDecimal totalSize;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
