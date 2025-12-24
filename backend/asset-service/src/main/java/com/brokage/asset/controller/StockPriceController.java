package com.brokage.asset.controller;

import com.brokage.asset.dto.StockPriceDTO;
import com.brokage.asset.entity.StockPrice;
import com.brokage.asset.repository.StockPriceRepository;
import com.brokage.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for Stock Prices (Market Simulator).
 *
 * This is separate from the core Asset API required by PDF.
 * Provides real-time stock price information for the frontend.
 */
@RestController
@RequestMapping("/api/stocks")
@RequiredArgsConstructor
public class StockPriceController {

    private final StockPriceRepository stockPriceRepository;

    @GetMapping
    public ResponseEntity<List<StockPriceDTO>> getAllStocks() {
        List<StockPriceDTO> stocks = stockPriceRepository.findAllByOrderBySymbol()
                .stream()
                .map(this::toDTO)
                .toList();
        return ResponseEntity.ok(stocks);
    }

    @GetMapping("/{symbol}")
    public ResponseEntity<StockPriceDTO> getStockBySymbol(@PathVariable String symbol) {
        StockPrice stock = stockPriceRepository.findById(symbol.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Stock", symbol));
        return ResponseEntity.ok(toDTO(stock));
    }

    @GetMapping("/exchange/{exchange}")
    public ResponseEntity<List<StockPriceDTO>> getStocksByExchange(@PathVariable String exchange) {
        List<StockPriceDTO> stocks = stockPriceRepository.findByExchangeOrderBySymbol(exchange.toUpperCase())
                .stream()
                .map(this::toDTO)
                .toList();
        return ResponseEntity.ok(stocks);
    }

    private StockPriceDTO toDTO(StockPrice stock) {
        return StockPriceDTO.builder()
                .symbol(stock.getSymbol())
                .name(stock.getName())
                .exchange(stock.getExchange())
                .price(stock.getPrice())
                .previousClose(stock.getPreviousClose())
                .dayHigh(stock.getDayHigh())
                .dayLow(stock.getDayLow())
                .bid(stock.getBid())
                .ask(stock.getAsk())
                .changePercent(stock.getChangePercent())
                .volume(stock.getVolume())
                .lastUpdated(stock.getLastUpdated())
                .build();
    }
}
