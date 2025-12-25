package com.brokage.processor.service;

import com.brokage.common.enums.OrderSide;
import com.brokage.common.exception.ResourceNotFoundException;
import com.brokage.processor.dto.TradeDTO;
import com.brokage.processor.entity.Trade;
import com.brokage.processor.repository.TradeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TradeServiceTest {

    @Mock
    private TradeRepository tradeRepository;

    @InjectMocks
    private TradeService tradeService;

    private UUID buyOrderId;
    private UUID sellOrderId;
    private UUID buyerCustomerId;
    private UUID sellerCustomerId;

    @BeforeEach
    void setUp() {
        buyOrderId = UUID.randomUUID();
        sellOrderId = UUID.randomUUID();
        buyerCustomerId = UUID.randomUUID();
        sellerCustomerId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("Save Trade Tests")
    class SaveTradeTests {

        @Test
        @DisplayName("Should save trade successfully")
        void shouldSaveTradeSuccessfully() {
            // Given
            Trade trade = createTrade();

            when(tradeRepository.save(any(Trade.class))).thenAnswer(inv -> {
                Trade t = inv.getArgument(0);
                t.setId(UUID.randomUUID());
                return t;
            });

            // When
            TradeDTO result = tradeService.saveTrade(trade);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getBuyOrderId()).isEqualTo(buyOrderId);
            assertThat(result.getSellOrderId()).isEqualTo(sellOrderId);
            verify(tradeRepository).save(trade);
        }
    }

    @Nested
    @DisplayName("Get Trade Tests")
    class GetTradeTests {

        @Test
        @DisplayName("Should get trade by ID")
        void shouldGetTradeById() {
            // Given
            UUID tradeId = UUID.randomUUID();
            Trade trade = createTrade();
            trade.setId(tradeId);

            when(tradeRepository.findById(tradeId)).thenReturn(Optional.of(trade));

            // When
            TradeDTO result = tradeService.getTradeById(tradeId);

            // Then
            assertThat(result.getId()).isEqualTo(tradeId);
            assertThat(result.getAssetName()).isEqualTo("AAPL");
        }

        @Test
        @DisplayName("Should throw exception when trade not found")
        void shouldThrowExceptionWhenTradeNotFound() {
            // Given
            UUID tradeId = UUID.randomUUID();
            when(tradeRepository.findById(tradeId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> tradeService.getTradeById(tradeId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Get Trades by Customer Tests")
    class GetTradesByCustomerTests {

        @Test
        @DisplayName("Should get trades by customer")
        void shouldGetTradesByCustomer() {
            // Given
            List<Trade> trades = List.of(createTrade(), createTrade());
            Page<Trade> tradePage = new PageImpl<>(trades);

            when(tradeRepository.findByCustomerId(eq(buyerCustomerId), any(Pageable.class)))
                    .thenReturn(tradePage);

            // When
            var result = tradeService.getTradesByCustomer(buyerCustomerId, 0, 10);

            // Then
            assertThat(result.getContent()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Get Trades by Asset Tests")
    class GetTradesByAssetTests {

        @Test
        @DisplayName("Should get trades by asset")
        void shouldGetTradesByAsset() {
            // Given
            List<Trade> trades = List.of(createTrade());
            Page<Trade> tradePage = new PageImpl<>(trades);

            when(tradeRepository.findByAssetName(eq("AAPL"), any(Pageable.class)))
                    .thenReturn(tradePage);

            // When
            var result = tradeService.getTradesByAsset("AAPL", 0, 10);

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getAssetName()).isEqualTo("AAPL");
        }
    }

    @Nested
    @DisplayName("Get Trades by Date Range Tests")
    class GetTradesByDateRangeTests {

        @Test
        @DisplayName("Should get trades by date range")
        void shouldGetTradesByDateRange() {
            // Given
            LocalDateTime startDate = LocalDateTime.now().minusDays(7);
            LocalDateTime endDate = LocalDateTime.now();

            List<Trade> trades = List.of(createTrade());
            Page<Trade> tradePage = new PageImpl<>(trades);

            when(tradeRepository.findByCreatedAtBetween(eq(startDate), eq(endDate), any(Pageable.class)))
                    .thenReturn(tradePage);

            // When
            var result = tradeService.getTradesByDateRange(startDate, endDate, 0, 10);

            // Then
            assertThat(result.getContent()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Count Tests")
    class CountTests {

        @Test
        @DisplayName("Should count trades by asset")
        void shouldCountTradesByAsset() {
            // Given
            when(tradeRepository.countByAssetName("AAPL")).thenReturn(100L);

            // When
            long count = tradeService.getTradeCountByAsset("AAPL");

            // Then
            assertThat(count).isEqualTo(100L);
        }
    }

    private Trade createTrade() {
        return Trade.builder()
                .buyOrderId(buyOrderId)
                .sellOrderId(sellOrderId)
                .buyerCustomerId(buyerCustomerId)
                .sellerCustomerId(sellerCustomerId)
                .assetName("AAPL")
                .quantity(new BigDecimal("10"))
                .price(new BigDecimal("150.00"))
                .totalValue(new BigDecimal("1500.00"))
                .takerSide(OrderSide.BUY)
                .build();
    }
}
