package com.brokage.processor.controller;

import com.brokage.common.dto.ApiResponse;
import com.brokage.processor.dto.TradeDTO;
import com.brokage.processor.service.MatchingEngineService;
import com.brokage.processor.service.TradeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/trades")
@RequiredArgsConstructor
@Slf4j
public class TradeController {

    private final TradeService tradeService;
    private final MatchingEngineService matchingEngineService;

    @GetMapping("/{tradeId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TradeDTO>> getTradeById(@PathVariable UUID tradeId) {
        log.debug("Getting trade by ID: {}", tradeId);
        TradeDTO trade = tradeService.getTradeById(tradeId);
        return ResponseEntity.ok(ApiResponse.success(trade));
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasRole('ADMIN') or @tradeSecurityService.isOwnCustomer(#customerId, authentication)")
    public ResponseEntity<ApiResponse<Page<TradeDTO>>> getCustomerTrades(
            @PathVariable UUID customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.debug("Getting trades for customer {}", customerId);
        Page<TradeDTO> trades = tradeService.getTradesByCustomer(customerId, page, size);
        return ResponseEntity.ok(ApiResponse.success(trades));
    }

    @GetMapping("/asset/{assetName}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<TradeDTO>>> getAssetTrades(
            @PathVariable String assetName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.debug("Getting trades for asset {}", assetName);
        Page<TradeDTO> trades = tradeService.getTradesByAsset(assetName, page, size);
        return ResponseEntity.ok(ApiResponse.success(trades));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<TradeDTO>>> getTrades(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.debug("Getting trades between {} and {}", startDate, endDate);

        if (startDate == null) {
            startDate = LocalDateTime.now().minusDays(30);
        }
        if (endDate == null) {
            endDate = LocalDateTime.now();
        }

        Page<TradeDTO> trades = tradeService.getTradesByDateRange(startDate, endDate, page, size);
        return ResponseEntity.ok(ApiResponse.success(trades));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats() {
        log.debug("Getting trade stats");

        Map<String, Object> stats = Map.of(
                "activeOrdersInQueue", matchingEngineService.getActiveOrderCount()
        );

        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/stats/{assetName}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAssetStats(@PathVariable String assetName) {
        log.debug("Getting stats for asset {}", assetName);

        Map<String, Object> stats = Map.of(
                "totalTrades", tradeService.getTradeCountByAsset(assetName),
                "activeOrdersInQueue", matchingEngineService.getActiveOrderCountByAsset(assetName)
        );

        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}
