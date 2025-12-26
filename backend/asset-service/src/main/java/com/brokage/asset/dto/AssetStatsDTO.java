package com.brokage.asset.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetStatsDTO {
    private BigDecimal totalAUM;           // Total Assets Under Management
    private BigDecimal totalTryBalance;    // Total TRY across all customers
    private long totalCustomersWithAssets; // Number of customers who have assets
    private long uniqueAssetTypes;         // Number of unique asset types (e.g., TRY, AAPL, GOOGL)
    private BigDecimal largestPosition;    // Largest single position value
}
