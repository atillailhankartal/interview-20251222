package com.brokage.asset.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
public class PriceUpdateEvent extends ApplicationEvent {

    private final List<PriceUpdate> updates;

    public PriceUpdateEvent(Object source, List<PriceUpdate> updates) {
        super(source);
        this.updates = updates;
    }

    @Data
    @Builder
    @AllArgsConstructor
    public static class PriceUpdate {
        private String symbol;
        private BigDecimal price;
        private BigDecimal bid;
        private BigDecimal ask;
        private BigDecimal changePercent;
        private BigDecimal dayHigh;
        private BigDecimal dayLow;
        private Long volume;
        private LocalDateTime updatedAt;
    }
}
