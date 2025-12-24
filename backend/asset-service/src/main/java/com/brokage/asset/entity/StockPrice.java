package com.brokage.asset.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Real-time stock price data for Market Simulator.
 *
 * This is SEPARATE from Asset entity:
 * - Asset = PDF core entity (tradeable assets master data)
 * - StockPrice = Market Simulator entity (real-time price display)
 *
 * In production, this data would come from external APIs (AlphaVantage, Yahoo Finance).
 * For demo, MarketSimulator generates simulated price movements.
 */
@Entity
@Table(name = "stock_prices", indexes = {
        @Index(name = "idx_stock_price_symbol", columnList = "symbol", unique = true),
        @Index(name = "idx_stock_price_exchange", columnList = "exchange")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockPrice {

    @Id
    @Column(name = "symbol", length = 20)
    private String symbol;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "exchange", length = 20)
    private String exchange;

    @Column(name = "price", precision = 19, scale = 4)
    private BigDecimal price;

    @Column(name = "previous_close", precision = 19, scale = 4)
    private BigDecimal previousClose;

    @Column(name = "day_high", precision = 19, scale = 4)
    private BigDecimal dayHigh;

    @Column(name = "day_low", precision = 19, scale = 4)
    private BigDecimal dayLow;

    @Column(name = "bid", precision = 19, scale = 4)
    private BigDecimal bid;

    @Column(name = "ask", precision = 19, scale = 4)
    private BigDecimal ask;

    @Column(name = "change_percent", precision = 10, scale = 4)
    private BigDecimal changePercent;

    @Column(name = "volume")
    @Builder.Default
    private Long volume = 0L;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    /**
     * Calculate daily change percentage from previousClose
     */
    public void calculateChangePercent() {
        if (previousClose != null && previousClose.compareTo(BigDecimal.ZERO) > 0 && price != null) {
            this.changePercent = price.subtract(previousClose)
                    .divide(previousClose, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }
    }

    /**
     * Update bid/ask based on current price (spread: 0.1%)
     */
    public void updateBidAsk() {
        if (price != null) {
            this.bid = price.multiply(new BigDecimal("0.999")).setScale(4, RoundingMode.HALF_UP);
            this.ask = price.multiply(new BigDecimal("1.001")).setScale(4, RoundingMode.HALF_UP);
        }
    }
}
