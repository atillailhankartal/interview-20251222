package com.brokage.processor.service;

import com.brokage.common.enums.OrderSide;
import com.brokage.processor.dto.MatchOrderRequest;
import com.brokage.processor.dto.TradeDTO;
import com.brokage.processor.entity.MatchingQueue;
import com.brokage.processor.entity.OutboxEvent;
import com.brokage.processor.entity.Trade;
import com.brokage.processor.repository.MatchingQueueRepository;
import com.brokage.processor.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchingEngineService {

    private final MatchingQueueRepository matchingQueueRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final TradeService tradeService;
    private final ObjectMapper objectMapper;

    @Transactional
    public void addToQueue(MatchOrderRequest request) {
        log.info("Adding order {} to matching queue", request.getOrderId());

        if (matchingQueueRepository.existsByOrderId(request.getOrderId())) {
            log.warn("Order {} already exists in queue, skipping", request.getOrderId());
            return;
        }

        MatchingQueue queueEntry = MatchingQueue.builder()
                .orderId(request.getOrderId())
                .customerId(request.getCustomerId())
                .assetName(request.getAssetName())
                .orderSide(request.getOrderSide())
                .price(request.getPrice())
                .remainingSize(request.getSize())
                .tierPriority(request.getTierPriority() != null ? request.getTierPriority() : 0)
                .status(MatchingQueue.QueueStatus.ACTIVE)
                .queuedAt(LocalDateTime.now())
                .build();

        matchingQueueRepository.save(queueEntry);
        log.info("Order {} added to queue", request.getOrderId());

        tryMatch(queueEntry);
    }

    @Transactional
    public List<TradeDTO> tryMatch(MatchingQueue takerOrder) {
        log.info("Attempting to match order {}", takerOrder.getOrderId());
        List<TradeDTO> trades = new ArrayList<>();

        List<MatchingQueue> makerOrders = findMatchingOrders(takerOrder);

        for (MatchingQueue makerOrder : makerOrders) {
            if (!takerOrder.isActive()) {
                break;
            }

            TradeDTO trade = executeTrade(takerOrder, makerOrder);
            if (trade != null) {
                trades.add(trade);
                publishTradeEvent(trade, takerOrder, makerOrder);
            }
        }

        matchingQueueRepository.save(takerOrder);
        log.info("Matching completed for order {}, {} trades executed",
                takerOrder.getOrderId(), trades.size());

        return trades;
    }

    @Transactional
    public void cancelOrder(UUID orderId, String reason) {
        log.info("Cancelling order {} from queue, reason: {}", orderId, reason);

        matchingQueueRepository.findByOrderId(orderId).ifPresent(queueEntry -> {
            queueEntry.cancel(reason);
            matchingQueueRepository.save(queueEntry);
            log.info("Order {} cancelled from queue", orderId);
        });
    }

    private List<MatchingQueue> findMatchingOrders(MatchingQueue takerOrder) {
        if (takerOrder.getOrderSide() == OrderSide.BUY) {
            return matchingQueueRepository.findSellOrdersForBuy(
                    takerOrder.getAssetName(),
                    takerOrder.getPrice()
            );
        } else {
            return matchingQueueRepository.findBuyOrdersForSell(
                    takerOrder.getAssetName(),
                    takerOrder.getPrice()
            );
        }
    }

    private TradeDTO executeTrade(MatchingQueue takerOrder, MatchingQueue makerOrder) {
        BigDecimal matchQuantity = takerOrder.getRemainingSize()
                .min(makerOrder.getRemainingSize());

        if (matchQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        BigDecimal tradePrice = makerOrder.getPrice();

        UUID buyOrderId, sellOrderId, buyerCustomerId, sellerCustomerId;
        if (takerOrder.getOrderSide() == OrderSide.BUY) {
            buyOrderId = takerOrder.getOrderId();
            sellOrderId = makerOrder.getOrderId();
            buyerCustomerId = takerOrder.getCustomerId();
            sellerCustomerId = makerOrder.getCustomerId();
        } else {
            buyOrderId = makerOrder.getOrderId();
            sellOrderId = takerOrder.getOrderId();
            buyerCustomerId = makerOrder.getCustomerId();
            sellerCustomerId = takerOrder.getCustomerId();
        }

        Trade trade = Trade.create(
                buyOrderId, sellOrderId,
                buyerCustomerId, sellerCustomerId,
                takerOrder.getAssetName(),
                matchQuantity, tradePrice,
                takerOrder.getOrderSide()
        );

        TradeDTO savedTrade = tradeService.saveTrade(trade);

        takerOrder.partialMatch(matchQuantity);
        makerOrder.partialMatch(matchQuantity);
        matchingQueueRepository.save(makerOrder);

        log.info("Trade executed: {} {} @ {} between orders {} and {}",
                matchQuantity, takerOrder.getAssetName(), tradePrice,
                takerOrder.getOrderId(), makerOrder.getOrderId());

        return savedTrade;
    }

    private void publishTradeEvent(TradeDTO trade, MatchingQueue takerOrder, MatchingQueue makerOrder) {
        try {
            Map<String, Object> eventPayload = new java.util.HashMap<>();
            eventPayload.put("eventType", "OrderMatchedEvent");
            eventPayload.put("eventId", UUID.randomUUID().toString());
            eventPayload.put("tradeId", trade.getId().toString());
            eventPayload.put("buyOrderId", trade.getBuyOrderId().toString());
            eventPayload.put("sellOrderId", trade.getSellOrderId().toString());
            eventPayload.put("buyerCustomerId", trade.getBuyerCustomerId().toString());
            eventPayload.put("sellerCustomerId", trade.getSellerCustomerId().toString());
            eventPayload.put("assetName", trade.getAssetName());
            eventPayload.put("quantity", trade.getQuantity().toString());
            eventPayload.put("price", trade.getPrice().toString());
            eventPayload.put("totalValue", trade.getTotalValue().toString());
            eventPayload.put("takerSide", trade.getTakerSide().toString());
            eventPayload.put("timestamp", LocalDateTime.now().toString());

            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateId(trade.getId())
                    .aggregateType("Trade")
                    .eventType("OrderMatchedEvent")
                    .topic("order-events")
                    .partitionKey(trade.getAssetName())
                    .payload(objectMapper.writeValueAsString(eventPayload))
                    .build();

            outboxEventRepository.save(outboxEvent);
            log.debug("Trade event published to outbox for trade {}", trade.getId());
        } catch (Exception e) {
            log.error("Failed to publish trade event: {}", e.getMessage(), e);
        }
    }

    public long getActiveOrderCount() {
        return matchingQueueRepository.countActiveOrders();
    }

    public long getActiveOrderCountByAsset(String assetName) {
        return matchingQueueRepository.countActiveOrdersByAsset(assetName);
    }
}
