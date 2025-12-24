package com.brokage.asset.dto;

import com.brokage.common.enums.AssetType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetDTO {

    private Long id;
    private String symbol;
    private String name;
    private AssetType type;
    private String description;
    private BigDecimal minOrderQuantity;
    private BigDecimal lotSize;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
