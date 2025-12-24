package com.brokage.asset.config;

import com.brokage.asset.entity.StockPrice;
import com.brokage.asset.repository.StockPriceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Seeds initial stock prices for Market Simulator.
 * Only runs in demo/dev profiles.
 */
@Component
@Profile({"demo", "dev", "docker"})
@Order(2) // Run after other seeders
@RequiredArgsConstructor
@Slf4j
public class StockPriceSeeder implements CommandLineRunner {

    private final StockPriceRepository stockPriceRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (stockPriceRepository.count() > 0) {
            log.info("Stock prices already seeded, skipping...");
            return;
        }

        log.info("Seeding stock prices for Market Simulator...");

        List<StockPrice> stocks = List.of(
                // US Stocks (NASDAQ)
                createStock("AAPL", "Apple Inc.", "NASDAQ",
                        new BigDecimal("178.50"), new BigDecimal("177.25")),

                createStock("GOOGL", "Alphabet Inc.", "NASDAQ",
                        new BigDecimal("141.25"), new BigDecimal("140.50")),

                createStock("MSFT", "Microsoft Corporation", "NASDAQ",
                        new BigDecimal("378.90"), new BigDecimal("375.00")),

                createStock("NVDA", "NVIDIA Corporation", "NASDAQ",
                        new BigDecimal("495.50"), new BigDecimal("490.00")),

                createStock("AMZN", "Amazon.com Inc.", "NASDAQ",
                        new BigDecimal("178.25"), new BigDecimal("176.00")),

                createStock("META", "Meta Platforms Inc.", "NASDAQ",
                        new BigDecimal("505.75"), new BigDecimal("500.00")),

                createStock("TSLA", "Tesla Inc.", "NASDAQ",
                        new BigDecimal("248.50"), new BigDecimal("245.00")),

                // BIST (Istanbul Stock Exchange)
                createStock("THYAO", "Turk Hava Yollari", "BIST",
                        new BigDecimal("285.40"), new BigDecimal("282.00")),

                createStock("GARAN", "Garanti BBVA", "BIST",
                        new BigDecimal("52.75"), new BigDecimal("51.90")),

                createStock("AKBNK", "Akbank", "BIST",
                        new BigDecimal("48.20"), new BigDecimal("47.50")),

                createStock("SISE", "Sise Cam", "BIST",
                        new BigDecimal("42.80"), new BigDecimal("42.00")),

                createStock("EREGL", "Eregli Demir Celik", "BIST",
                        new BigDecimal("58.90"), new BigDecimal("58.00")),

                createStock("KCHOL", "Koc Holding", "BIST",
                        new BigDecimal("178.50"), new BigDecimal("175.00")),

                createStock("SAHOL", "Sabanci Holding", "BIST",
                        new BigDecimal("85.40"), new BigDecimal("84.00")),

                createStock("TUPRS", "Tupras", "BIST",
                        new BigDecimal("165.80"), new BigDecimal("163.00"))
        );

        stockPriceRepository.saveAll(stocks);
        log.info("Seeded {} stock prices", stocks.size());
    }

    private StockPrice createStock(String symbol, String name, String exchange,
                                   BigDecimal price, BigDecimal previousClose) {
        StockPrice stock = StockPrice.builder()
                .symbol(symbol)
                .name(name)
                .exchange(exchange)
                .price(price)
                .previousClose(previousClose)
                .dayHigh(price)
                .dayLow(price)
                .volume(0L)
                .lastUpdated(LocalDateTime.now())
                .build();

        stock.updateBidAsk();
        stock.calculateChangePercent();

        return stock;
    }
}
