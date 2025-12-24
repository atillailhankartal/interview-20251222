package com.brokage.asset.dto;

import com.brokage.asset.entity.AssetReservation.ReservationStatus;
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
public class AssetReservationDTO {

    private UUID id;
    private UUID customerId;
    private UUID orderId;
    private String assetSymbol;
    private BigDecimal reservedAmount;
    private ReservationStatus status;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime releasedAt;
    private String releaseReason;
}
