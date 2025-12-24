package com.brokage.asset.service;

import com.brokage.asset.dto.PriceUpdateEvent;
import com.brokage.asset.entity.StockPrice;
import com.brokage.asset.repository.StockPriceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Market Simulator - Simulates real-time stock price movements.
 *
 * This is a DEMO feature to showcase:
 * - SSE (Server-Sent Events) for real-time updates
 * - Understanding of real brokerage systems
 * - Product thinking beyond basic requirements
 *
 * NOTE: This is separate from PDF core requirements.
 * In production, prices would come from external APIs (AlphaVantage, Yahoo Finance).
 */
@Service
@Profile({"demo", "dev", "docker"})
@RequiredArgsConstructor
@Slf4j
public class MarketSimulator {

    private final StockPriceRepository stockPriceRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Random random = new Random();

    // Volatility per exchange (max % change per tick)
    private static final Map<String, Double> VOLATILITY = Map.of(
            "NASDAQ", 0.005,  // 0.5%
            "BIST", 0.008     // 0.8% (more volatile)
    );

    private static final double DEFAULT_VOLATILITY = 0.003;

    /**
     * Simulate price movements every 5 seconds
     * Uses Brownian Motion style random walk
     */
    @Scheduled(fixedRate = 5000)
    @Transactional
    public void simulatePriceMovements() {
        List<StockPrice> stocks = stockPriceRepository.findAll();

        if (stocks.isEmpty()) {
            return;
        }

        List<PriceUpdateEvent.PriceUpdate> updates = new ArrayList<>();

        for (StockPrice stock : stocks) {
            BigDecimal oldPrice = stock.getPrice();
            if (oldPrice == null || oldPrice.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            double volatility = VOLATILITY.getOrDefault(stock.getExchange(), DEFAULT_VOLATILITY);

            // Random walk: -volatility to +volatility
            double change = (random.nextDouble() * 2 - 1) * volatility;
            BigDecimal multiplier = BigDecimal.ONE.add(BigDecimal.valueOf(change));

            BigDecimal newPrice = oldPrice
                    .multiply(multiplier)
                    .setScale(4, RoundingMode.HALF_UP);

            // Update day high/low
            if (stock.getDayHigh() == null || newPrice.compareTo(stock.getDayHigh()) > 0) {
                stock.setDayHigh(newPrice);
            }
            if (stock.getDayLow() == null || newPrice.compareTo(stock.getDayLow()) < 0) {
                stock.setDayLow(newPrice);
            }

            stock.setPrice(newPrice);
            stock.updateBidAsk();
            stock.calculateChangePercent();
            stock.setLastUpdated(LocalDateTime.now());

            // Simulate volume increase
            stock.setVolume(stock.getVolume() + random.nextInt(10000));

            // Collect update for SSE broadcast
            updates.add(PriceUpdateEvent.PriceUpdate.builder()
                    .symbol(stock.getSymbol())
                    .price(newPrice)
                    .bid(stock.getBid())
                    .ask(stock.getAsk())
                    .changePercent(stock.getChangePercent())
                    .dayHigh(stock.getDayHigh())
                    .dayLow(stock.getDayLow())
                    .volume(stock.getVolume())
                    .updatedAt(stock.getLastUpdated())
                    .build());
        }

        stockPriceRepository.saveAll(stocks);

        // Publish event for SSE subscribers
        if (!updates.isEmpty()) {
            eventPublisher.publishEvent(new PriceUpdateEvent(this, updates));
            log.debug("Published {} price updates", updates.size());
        }
    }

    /**
     * Reset daily stats at midnight
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void resetDailyStats() {
        List<StockPrice> stocks = stockPriceRepository.findAll();

        stocks.forEach(stock -> {
            stock.setPreviousClose(stock.getPrice());
            stock.setDayHigh(stock.getPrice());
            stock.setDayLow(stock.getPrice());
            stock.setVolume(0L);
            stock.calculateChangePercent();
        });

        stockPriceRepository.saveAll(stocks);
        log.info("Daily stats reset for {} stocks", stocks.size());
    }
}
