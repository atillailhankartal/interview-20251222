package com.brokage.order.service;

import com.brokage.common.enums.OrderSide;
import com.brokage.common.enums.OrderStatus;
import com.brokage.common.enums.OrderType;
import com.brokage.common.exception.BusinessException;
import com.brokage.common.exception.ResourceNotFoundException;
import com.brokage.order.client.CustomerClient;
import com.brokage.order.dto.CreateOrderRequest;
import com.brokage.order.dto.OrderDTO;
import com.brokage.order.dto.OrderFilterRequest;
import com.brokage.order.entity.Order;
import com.brokage.order.entity.OutboxEvent;
import com.brokage.order.repository.OrderRepository;
import com.brokage.order.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private CustomerClient customerClient;

    @InjectMocks
    private OrderService orderService;

    private UUID customerId;
    private UUID orderId;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        orderId = UUID.randomUUID();
        // Default: customer is orderable (lenient to avoid unnecessary stubbing errors)
        lenient().when(customerClient.isCustomerOrderable(any())).thenReturn(true);
    }

    @Nested
    @DisplayName("Create Order Tests")
    class CreateOrderTests {

        @Test
        @DisplayName("Should create BUY order successfully")
        void shouldCreateBuyOrderSuccessfully() throws Exception {
            // Given
            CreateOrderRequest request = CreateOrderRequest.builder()
                    .customerId(customerId)
                    .assetName("AAPL")
                    .orderSide(OrderSide.BUY)
                    .size(BigDecimal.TEN)
                    .price(BigDecimal.valueOf(150))
                    .build();

            Order savedOrder = createOrder(orderId, customerId, "AAPL", OrderSide.BUY,
                    BigDecimal.TEN, BigDecimal.valueOf(150), OrderStatus.PENDING);

            when(orderRepository.saveAndFlush(any(Order.class))).thenReturn(savedOrder);
            when(objectMapper.writeValueAsString(any())).thenReturn("{}");

            // When
            OrderDTO result = orderService.createOrder(request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getCustomerId()).isEqualTo(customerId);
            assertThat(result.getAssetName()).isEqualTo("AAPL");
            assertThat(result.getOrderSide()).isEqualTo(OrderSide.BUY);
            assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING);
            assertThat(result.getTotalValue()).isEqualTo(BigDecimal.valueOf(1500));

            verify(orderRepository).saveAndFlush(any(Order.class));
            verify(outboxEventRepository).save(any(OutboxEvent.class));
        }

        @Test
        @DisplayName("Should create SELL order successfully")
        void shouldCreateSellOrderSuccessfully() throws Exception {
            // Given
            CreateOrderRequest request = CreateOrderRequest.builder()
                    .customerId(customerId)
                    .assetName("GOOGL")
                    .orderSide(OrderSide.SELL)
                    .size(BigDecimal.valueOf(5))
                    .price(BigDecimal.valueOf(100))
                    .build();

            Order savedOrder = createOrder(orderId, customerId, "GOOGL", OrderSide.SELL,
                    BigDecimal.valueOf(5), BigDecimal.valueOf(100), OrderStatus.PENDING);

            when(orderRepository.saveAndFlush(any(Order.class))).thenReturn(savedOrder);
            when(objectMapper.writeValueAsString(any())).thenReturn("{}");

            // When
            OrderDTO result = orderService.createOrder(request);

            // Then
            assertThat(result.getOrderSide()).isEqualTo(OrderSide.SELL);
            assertThat(result.getTotalValue()).isEqualTo(BigDecimal.valueOf(500));
        }

        @Test
        @DisplayName("Should return existing order for duplicate idempotency key")
        void shouldReturnExistingOrderForDuplicateIdempotencyKey() {
            // Given
            String idempotencyKey = "unique-key-123";
            CreateOrderRequest request = CreateOrderRequest.builder()
                    .customerId(customerId)
                    .assetName("AAPL")
                    .orderSide(OrderSide.BUY)
                    .size(BigDecimal.TEN)
                    .price(BigDecimal.valueOf(150))
                    .idempotencyKey(idempotencyKey)
                    .build();

            Order existingOrder = createOrder(orderId, customerId, "AAPL", OrderSide.BUY,
                    BigDecimal.TEN, BigDecimal.valueOf(150), OrderStatus.PENDING);

            when(orderRepository.findByIdempotencyKey(idempotencyKey))
                    .thenReturn(Optional.of(existingOrder));

            // When
            OrderDTO result = orderService.createOrder(request);

            // Then
            assertThat(result.getId()).isEqualTo(orderId);
            verify(orderRepository, never()).saveAndFlush(any(Order.class));
            verify(outboxEventRepository, never()).save(any(OutboxEvent.class));
        }
    }

    @Nested
    @DisplayName("Get Order Tests")
    class GetOrderTests {

        @Test
        @DisplayName("Should get order by ID")
        void shouldGetOrderById() {
            // Given
            Order order = createOrder(orderId, customerId, "AAPL", OrderSide.BUY,
                    BigDecimal.TEN, BigDecimal.valueOf(150), OrderStatus.PENDING);

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

            // When
            OrderDTO result = orderService.getOrderById(orderId);

            // Then
            assertThat(result.getId()).isEqualTo(orderId);
            assertThat(result.getAssetName()).isEqualTo("AAPL");
        }

        @Test
        @DisplayName("Should throw exception when order not found")
        void shouldThrowExceptionWhenOrderNotFound() {
            // Given
            when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> orderService.getOrderById(orderId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Should get order for customer with ownership check")
        void shouldGetOrderForCustomerWithOwnershipCheck() {
            // Given
            Order order = createOrder(orderId, customerId, "AAPL", OrderSide.BUY,
                    BigDecimal.TEN, BigDecimal.valueOf(150), OrderStatus.PENDING);

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

            // When
            OrderDTO result = orderService.getOrderByIdForCustomer(orderId, customerId);

            // Then
            assertThat(result.getId()).isEqualTo(orderId);
        }

        @Test
        @DisplayName("Should throw exception when customer does not own order")
        void shouldThrowExceptionWhenCustomerDoesNotOwnOrder() {
            // Given
            UUID otherCustomerId = UUID.randomUUID();
            Order order = createOrder(orderId, customerId, "AAPL", OrderSide.BUY,
                    BigDecimal.TEN, BigDecimal.valueOf(150), OrderStatus.PENDING);

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

            // When & Then
            assertThatThrownBy(() -> orderService.getOrderByIdForCustomer(orderId, otherCustomerId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("permission");
        }
    }

    @Nested
    @DisplayName("Cancel Order Tests")
    class CancelOrderTests {

        @Test
        @DisplayName("Should cancel PENDING order successfully")
        void shouldCancelPendingOrderSuccessfully() throws Exception {
            // Given
            Order order = createOrder(orderId, customerId, "AAPL", OrderSide.BUY,
                    BigDecimal.TEN, BigDecimal.valueOf(150), OrderStatus.PENDING);

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
            when(orderRepository.save(any(Order.class))).thenReturn(order);
            when(objectMapper.writeValueAsString(any())).thenReturn("{}");

            // When
            OrderDTO result = orderService.cancelOrder(orderId);

            // Then
            assertThat(result.getStatus()).isEqualTo(OrderStatus.CANCELED);
            verify(outboxEventRepository).save(any(OutboxEvent.class));
        }

        @Test
        @DisplayName("Should throw exception when cancelling non-PENDING order")
        void shouldThrowExceptionWhenCancellingNonPendingOrder() {
            // Given
            Order order = createOrder(orderId, customerId, "AAPL", OrderSide.BUY,
                    BigDecimal.TEN, BigDecimal.valueOf(150), OrderStatus.MATCHED);

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

            // When & Then
            assertThatThrownBy(() -> orderService.cancelOrder(orderId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("cannot be cancelled");
        }

        @Test
        @DisplayName("Should cancel order for customer with ownership check")
        void shouldCancelOrderForCustomerWithOwnershipCheck() throws Exception {
            // Given
            Order order = createOrder(orderId, customerId, "AAPL", OrderSide.BUY,
                    BigDecimal.TEN, BigDecimal.valueOf(150), OrderStatus.PENDING);

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
            when(orderRepository.save(any(Order.class))).thenReturn(order);
            when(objectMapper.writeValueAsString(any())).thenReturn("{}");

            // When
            OrderDTO result = orderService.cancelOrderForCustomer(orderId, customerId);

            // Then
            assertThat(result.getStatus()).isEqualTo(OrderStatus.CANCELED);
        }
    }

    @Nested
    @DisplayName("Match Order Tests")
    class MatchOrderTests {

        @Test
        @DisplayName("Should match PENDING order successfully")
        void shouldMatchPendingOrderSuccessfully() throws Exception {
            // Given
            Order order = createOrder(orderId, customerId, "AAPL", OrderSide.BUY,
                    BigDecimal.TEN, BigDecimal.valueOf(150), OrderStatus.PENDING);

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
            when(orderRepository.save(any(Order.class))).thenReturn(order);
            when(objectMapper.writeValueAsString(any())).thenReturn("{}");

            // When
            OrderDTO result = orderService.matchOrder(orderId);

            // Then
            assertThat(result.getStatus()).isEqualTo(OrderStatus.MATCHED);
            assertThat(result.getFilledSize()).isEqualTo(BigDecimal.TEN);
            verify(outboxEventRepository).save(any(OutboxEvent.class));
        }

        @Test
        @DisplayName("Should throw exception when matching non-PENDING order")
        void shouldThrowExceptionWhenMatchingNonPendingOrder() {
            // Given
            Order order = createOrder(orderId, customerId, "AAPL", OrderSide.BUY,
                    BigDecimal.TEN, BigDecimal.valueOf(150), OrderStatus.CANCELED);

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

            // When & Then
            assertThatThrownBy(() -> orderService.matchOrder(orderId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("PENDING");
        }
    }

    @Nested
    @DisplayName("List Orders Tests")
    class ListOrdersTests {

        @Test
        @DisplayName("Should list orders with pagination")
        void shouldListOrdersWithPagination() {
            // Given
            OrderFilterRequest filter = new OrderFilterRequest();
            filter.setPage(0);
            filter.setSize(10);

            List<Order> orders = List.of(
                    createOrder(UUID.randomUUID(), customerId, "AAPL", OrderSide.BUY,
                            BigDecimal.TEN, BigDecimal.valueOf(150), OrderStatus.PENDING),
                    createOrder(UUID.randomUUID(), customerId, "GOOGL", OrderSide.SELL,
                            BigDecimal.valueOf(5), BigDecimal.valueOf(100), OrderStatus.MATCHED)
            );

            Page<Order> orderPage = new PageImpl<>(orders);
            when(orderRepository.findAllByFilters(any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                    .thenReturn(orderPage);

            // When
            var result = orderService.listOrders(filter);

            // Then
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(2);
        }
    }

    private Order createOrder(UUID id, UUID customerId, String assetName,
                              OrderSide orderSide, BigDecimal size, BigDecimal price,
                              OrderStatus status) {
        Order order = Order.builder()
                .customerId(customerId)
                .assetName(assetName)
                .orderSide(orderSide)
                .orderType(OrderType.LIMIT)
                .size(size)
                .price(price)
                .filledSize(BigDecimal.ZERO)
                .status(status)
                .build();

        // Use reflection to set ID since it's auto-generated
        try {
            var idField = order.getClass().getSuperclass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(order, id);
        } catch (Exception e) {
            // Fallback - just return order without ID set
        }

        return order;
    }
}
