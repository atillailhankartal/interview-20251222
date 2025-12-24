package com.brokage.asset.dto;

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
public class StockPriceDTO {

    private String symbol;
    private String name;
    private String exchange;
    private BigDecimal price;
    private BigDecimal previousClose;
    private BigDecimal dayHigh;
    private BigDecimal dayLow;
    private BigDecimal bid;
    private BigDecimal ask;
    private BigDecimal changePercent;
    private Long volume;
    private LocalDateTime lastUpdated;
}
