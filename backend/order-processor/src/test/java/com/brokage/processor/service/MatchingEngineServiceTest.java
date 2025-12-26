package com.brokage.processor.service;

import com.brokage.common.enums.OrderSide;
import com.brokage.processor.dto.MatchOrderRequest;
import com.brokage.processor.dto.TradeDTO;
import com.brokage.processor.entity.MatchingQueue;
import com.brokage.processor.entity.Trade;
import com.brokage.processor.repository.MatchingQueueRepository;
import com.brokage.processor.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchingEngineServiceTest {

    @Mock
    private MatchingQueueRepository matchingQueueRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private TradeService tradeService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private MatchingEngineService matchingEngineService;

    private UUID customerId;
    private UUID orderId;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        orderId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("Add to Queue Tests")
    class AddToQueueTests {

        @Test
        @DisplayName("Should add order to queue")
        void shouldAddOrderToQueue() {
            // Given
            MatchOrderRequest request = createMatchRequest(orderId, customerId, "AAPL", OrderSide.BUY, "150.00", "10");

            when(matchingQueueRepository.existsByOrderId(orderId)).thenReturn(false);
            when(matchingQueueRepository.save(any(MatchingQueue.class))).thenAnswer(inv -> inv.getArgument(0));
            when(matchingQueueRepository.findSellOrdersForBuy(any(), any())).thenReturn(List.of());

            // When
            matchingEngineService.addToQueue(request);

            // Then
            verify(matchingQueueRepository, atLeastOnce()).save(any(MatchingQueue.class));
        }

        @Test
        @DisplayName("Should skip duplicate order")
        void shouldSkipDuplicateOrder() {
            // Given
            MatchOrderRequest request = createMatchRequest(orderId, customerId, "AAPL", OrderSide.BUY, "150.00", "10");

            when(matchingQueueRepository.existsByOrderId(orderId)).thenReturn(true);

            // When
            matchingEngineService.addToQueue(request);

            // Then
            verify(matchingQueueRepository, never()).save(any(MatchingQueue.class));
        }
    }

    @Nested
    @DisplayName("Order Matching Tests")
    class OrderMatchingTests {

        @Test
        @DisplayName("Should match buy order with sell order")
        void shouldMatchBuyOrderWithSellOrder() {
            // Given
            MatchingQueue buyOrder = createQueueEntry(orderId, customerId, "AAPL", OrderSide.BUY, "150.00", "10");
            MatchingQueue sellOrder = createQueueEntry(UUID.randomUUID(), UUID.randomUUID(), "AAPL", OrderSide.SELL, "150.00", "10");

            when(matchingQueueRepository.findSellOrdersForBuy("AAPL", new BigDecimal("150.00")))
                    .thenReturn(List.of(sellOrder));
            when(matchingQueueRepository.save(any(MatchingQueue.class))).thenAnswer(inv -> inv.getArgument(0));

            TradeDTO mockTrade = TradeDTO.builder()
                    .id(UUID.randomUUID())
                    .buyOrderId(orderId)
                    .sellOrderId(sellOrder.getOrderId())
                    .quantity(new BigDecimal("10"))
                    .price(new BigDecimal("150.00"))
                    .build();
            when(tradeService.saveTrade(any(Trade.class))).thenReturn(mockTrade);

            // When
            List<TradeDTO> trades = matchingEngineService.tryMatch(buyOrder);

            // Then
            assertThat(trades).hasSize(1);
            verify(tradeService).saveTrade(any(Trade.class));
        }

        @Test
        @DisplayName("Should match sell order with buy order")
        void shouldMatchSellOrderWithBuyOrder() {
            // Given
            MatchingQueue sellOrder = createQueueEntry(orderId, customerId, "AAPL", OrderSide.SELL, "150.00", "10");
            MatchingQueue buyOrder = createQueueEntry(UUID.randomUUID(), UUID.randomUUID(), "AAPL", OrderSide.BUY, "150.00", "10");

            when(matchingQueueRepository.findBuyOrdersForSell("AAPL", new BigDecimal("150.00")))
                    .thenReturn(List.of(buyOrder));
            when(matchingQueueRepository.save(any(MatchingQueue.class))).thenAnswer(inv -> inv.getArgument(0));

            TradeDTO mockTrade = TradeDTO.builder()
                    .id(UUID.randomUUID())
                    .buyOrderId(buyOrder.getOrderId())
                    .sellOrderId(orderId)
                    .quantity(new BigDecimal("10"))
                    .price(new BigDecimal("150.00"))
                    .build();
            when(tradeService.saveTrade(any(Trade.class))).thenReturn(mockTrade);

            // When
            List<TradeDTO> trades = matchingEngineService.tryMatch(sellOrder);

            // Then
            assertThat(trades).hasSize(1);
        }

        @Test
        @DisplayName("Should handle partial match")
        void shouldHandlePartialMatch() {
            // Given
            MatchingQueue buyOrder = createQueueEntry(orderId, customerId, "AAPL", OrderSide.BUY, "150.00", "20");
            MatchingQueue sellOrder = createQueueEntry(UUID.randomUUID(), UUID.randomUUID(), "AAPL", OrderSide.SELL, "150.00", "10");

            when(matchingQueueRepository.findSellOrdersForBuy("AAPL", new BigDecimal("150.00")))
                    .thenReturn(List.of(sellOrder));
            when(matchingQueueRepository.save(any(MatchingQueue.class))).thenAnswer(inv -> inv.getArgument(0));

            TradeDTO mockTrade = TradeDTO.builder()
                    .id(UUID.randomUUID())
                    .quantity(new BigDecimal("10"))
                    .price(new BigDecimal("150.00"))
                    .build();
            when(tradeService.saveTrade(any(Trade.class))).thenReturn(mockTrade);

            // When
            List<TradeDTO> trades = matchingEngineService.tryMatch(buyOrder);

            // Then
            assertThat(trades).hasSize(1);
            assertThat(buyOrder.getRemainingSize()).isEqualByComparingTo(new BigDecimal("10"));
        }

        @Test
        @DisplayName("Should not match when no matching orders")
        void shouldNotMatchWhenNoMatchingOrders() {
            // Given
            MatchingQueue buyOrder = createQueueEntry(orderId, customerId, "AAPL", OrderSide.BUY, "150.00", "10");

            when(matchingQueueRepository.findSellOrdersForBuy("AAPL", new BigDecimal("150.00")))
                    .thenReturn(List.of());

            // When
            List<TradeDTO> trades = matchingEngineService.tryMatch(buyOrder);

            // Then
            assertThat(trades).isEmpty();
            verify(tradeService, never()).saveTrade(any(Trade.class));
        }
    }

    @Nested
    @DisplayName("Cancel Order Tests")
    class CancelOrderTests {

        @Test
        @DisplayName("Should cancel order from queue")
        void shouldCancelOrderFromQueue() {
            // Given
            MatchingQueue queueEntry = createQueueEntry(orderId, customerId, "AAPL", OrderSide.BUY, "150.00", "10");

            when(matchingQueueRepository.findByOrderId(orderId)).thenReturn(Optional.of(queueEntry));
            when(matchingQueueRepository.save(any(MatchingQueue.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            matchingEngineService.cancelOrder(orderId, "User requested");

            // Then
            assertThat(queueEntry.getStatus()).isEqualTo(MatchingQueue.QueueStatus.CANCELED);
            verify(matchingQueueRepository).save(queueEntry);
        }

        @Test
        @DisplayName("Should handle cancel for non-existent order")
        void shouldHandleCancelForNonExistentOrder() {
            // Given
            when(matchingQueueRepository.findByOrderId(orderId)).thenReturn(Optional.empty());

            // When
            matchingEngineService.cancelOrder(orderId, "User requested");

            // Then
            verify(matchingQueueRepository, never()).save(any(MatchingQueue.class));
        }
    }

    @Nested
    @DisplayName("Statistics Tests")
    class StatisticsTests {

        @Test
        @DisplayName("Should get active order count")
        void shouldGetActiveOrderCount() {
            // Given
            when(matchingQueueRepository.countActiveOrders()).thenReturn(42L);

            // When
            long count = matchingEngineService.getActiveOrderCount();

            // Then
            assertThat(count).isEqualTo(42L);
        }

        @Test
        @DisplayName("Should get active order count by asset")
        void shouldGetActiveOrderCountByAsset() {
            // Given
            when(matchingQueueRepository.countActiveOrdersByAsset("AAPL")).thenReturn(10L);

            // When
            long count = matchingEngineService.getActiveOrderCountByAsset("AAPL");

            // Then
            assertThat(count).isEqualTo(10L);
        }
    }

    private MatchOrderRequest createMatchRequest(UUID orderId, UUID customerId, String symbol,
                                                  OrderSide side, String price, String size) {
        return MatchOrderRequest.builder()
                .orderId(orderId)
                .customerId(customerId)
                .assetName(symbol)
                .orderSide(side)
                .price(new BigDecimal(price))
                .size(new BigDecimal(size))
                .tierPriority(0)
                .build();
    }

    private MatchingQueue createQueueEntry(UUID orderId, UUID customerId, String symbol,
                                           OrderSide side, String price, String size) {
        return MatchingQueue.builder()
                .orderId(orderId)
                .customerId(customerId)
                .assetName(symbol)
                .orderSide(side)
                .price(new BigDecimal(price))
                .remainingSize(new BigDecimal(size))
                .tierPriority(0)
                .status(MatchingQueue.QueueStatus.ACTIVE)
                .queuedAt(LocalDateTime.now())
                .build();
    }
}
