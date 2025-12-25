package com.brokage.processor.service;

import com.brokage.common.exception.ResourceNotFoundException;
import com.brokage.processor.dto.TradeDTO;
import com.brokage.processor.entity.Trade;
import com.brokage.processor.repository.TradeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TradeService {

    private final TradeRepository tradeRepository;

    @Transactional
    public TradeDTO saveTrade(Trade trade) {
        log.info("Saving trade for buy order {} and sell order {}",
                trade.getBuyOrderId(), trade.getSellOrderId());
        Trade saved = tradeRepository.save(trade);
        log.info("Trade saved with ID: {}", saved.getId());
        return toDTO(saved);
    }

    public TradeDTO getTradeById(UUID tradeId) {
        Trade trade = tradeRepository.findById(tradeId)
                .orElseThrow(() -> new ResourceNotFoundException("Trade", tradeId.toString()));
        return toDTO(trade);
    }

    public Page<TradeDTO> getTradesByCustomer(UUID customerId, int page, int size) {
        log.debug("Getting trades for customer {}", customerId);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Trade> trades = tradeRepository.findByCustomerId(customerId, pageable);
        return trades.map(this::toDTO);
    }

    public Page<TradeDTO> getTradesByAsset(String assetName, int page, int size) {
        log.debug("Getting trades for asset {}", assetName);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Trade> trades = tradeRepository.findByAssetName(assetName, pageable);
        return trades.map(this::toDTO);
    }

    public Page<TradeDTO> getTradesByDateRange(LocalDateTime startDate, LocalDateTime endDate, int page, int size) {
        log.debug("Getting trades between {} and {}", startDate, endDate);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Trade> trades = tradeRepository.findByCreatedAtBetween(startDate, endDate, pageable);
        return trades.map(this::toDTO);
    }

    public long getTradeCountByAsset(String assetName) {
        return tradeRepository.countByAssetName(assetName);
    }

    private TradeDTO toDTO(Trade trade) {
        return TradeDTO.builder()
                .id(trade.getId())
                .buyOrderId(trade.getBuyOrderId())
                .sellOrderId(trade.getSellOrderId())
                .buyerCustomerId(trade.getBuyerCustomerId())
                .sellerCustomerId(trade.getSellerCustomerId())
                .assetName(trade.getAssetName())
                .quantity(trade.getQuantity())
                .price(trade.getPrice())
                .totalValue(trade.getTotalValue())
                .takerSide(trade.getTakerSide())
                .createdAt(trade.getCreatedAt())
                .build();
    }
}
