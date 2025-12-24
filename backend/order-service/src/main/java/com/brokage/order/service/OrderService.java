package com.brokage.order.service;

import com.brokage.common.enums.OrderStatus;
import com.brokage.common.enums.OrderType;
import com.brokage.common.exception.BusinessException;
import com.brokage.common.exception.ResourceNotFoundException;
import com.brokage.order.dto.CreateOrderRequest;
import com.brokage.order.dto.OrderDTO;
import com.brokage.order.dto.OrderFilterRequest;
import com.brokage.order.entity.Order;
import com.brokage.order.entity.OutboxEvent;
import com.brokage.order.repository.OrderRepository;
import com.brokage.order.repository.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    private static final String TOPIC_ORDER_CREATED = "order.created";
    private static final String TOPIC_ORDER_CANCELLED = "order.cancelled";
    private static final String TOPIC_ORDER_MATCHED = "order.matched";

    /**
     * Create a new order with idempotency support
     */
    @Transactional
    public OrderDTO createOrder(CreateOrderRequest request) {
        log.info("Creating order for customer: {}, asset: {}, side: {}",
                request.getCustomerId(), request.getAssetSymbol(), request.getOrderSide());

        // Idempotency check
        if (request.getIdempotencyKey() != null) {
            var existingOrder = orderRepository.findByIdempotencyKey(request.getIdempotencyKey());
            if (existingOrder.isPresent()) {
                log.info("Returning existing order for idempotency key: {}", request.getIdempotencyKey());
                return toDTO(existingOrder.get());
            }
        }

        // Create order entity
        Order order = Order.builder()
                .customerId(request.getCustomerId())
                .assetSymbol(request.getAssetSymbol().toUpperCase())
                .orderSide(request.getOrderSide())
                .orderType(request.getOrderType() != null ? request.getOrderType() : OrderType.LIMIT)
                .size(request.getSize())
                .price(request.getPrice())
                .status(OrderStatus.PENDING)
                .idempotencyKey(request.getIdempotencyKey())
                .build();

        Order savedOrder = orderRepository.save(order);
        log.info("Order created with ID: {}", savedOrder.getId());

        // Create outbox event for async processing
        createOutboxEvent(savedOrder, TOPIC_ORDER_CREATED, "OrderCreatedEvent");

        return toDTO(savedOrder);
    }

    /**
     * Get order by ID
     */
    @Transactional(readOnly = true)
    public OrderDTO getOrderById(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId.toString()));
        return toDTO(order);
    }

    /**
     * Get order by ID with ownership check
     */
    @Transactional(readOnly = true)
    public OrderDTO getOrderByIdForCustomer(UUID orderId, UUID customerId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId.toString()));

        if (!order.getCustomerId().equals(customerId)) {
            throw new BusinessException("You don't have permission to view this order");
        }

        return toDTO(order);
    }

    /**
     * List orders with filters (Admin access - all orders)
     */
    @Transactional(readOnly = true)
    public Page<OrderDTO> listOrders(OrderFilterRequest filter) {
        Pageable pageable = createPageable(filter);

        Page<Order> orders = orderRepository.findAllByFilters(
                filter.getCustomerId(),
                filter.getAssetSymbol(),
                filter.getStatus(),
                filter.getOrderSide(),
                filter.getStartDate(),
                filter.getEndDate(),
                pageable
        );

        return orders.map(this::toDTO);
    }

    /**
     * List orders for a specific customer
     */
    @Transactional(readOnly = true)
    public Page<OrderDTO> listOrdersForCustomer(UUID customerId, OrderFilterRequest filter) {
        Pageable pageable = createPageable(filter);

        Page<Order> orders = orderRepository.findByFilters(
                customerId,
                filter.getAssetSymbol(),
                filter.getStatus(),
                filter.getOrderSide(),
                filter.getStartDate(),
                filter.getEndDate(),
                pageable
        );

        return orders.map(this::toDTO);
    }

    /**
     * Cancel an order (only PENDING orders can be cancelled)
     */
    @Transactional
    public OrderDTO cancelOrder(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId.toString()));

        if (!order.isCancellable()) {
            throw new BusinessException(
                    String.format("Order cannot be cancelled. Current status: %s", order.getStatus()));
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(LocalDateTime.now());

        Order savedOrder = orderRepository.save(order);
        log.info("Order cancelled: {}", orderId);

        // Create outbox event for async processing (release blocked assets)
        createOutboxEvent(savedOrder, TOPIC_ORDER_CANCELLED, "OrderCancelledEvent");

        return toDTO(savedOrder);
    }

    /**
     * Cancel an order with ownership check
     */
    @Transactional
    public OrderDTO cancelOrderForCustomer(UUID orderId, UUID customerId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId.toString()));

        if (!order.getCustomerId().equals(customerId)) {
            throw new BusinessException("You don't have permission to cancel this order");
        }

        if (!order.isCancellable()) {
            throw new BusinessException(
                    String.format("Order cannot be cancelled. Current status: %s", order.getStatus()));
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(LocalDateTime.now());

        Order savedOrder = orderRepository.save(order);
        log.info("Order cancelled by customer: {}", orderId);

        // Create outbox event
        createOutboxEvent(savedOrder, TOPIC_ORDER_CANCELLED, "OrderCancelledEvent");

        return toDTO(savedOrder);
    }

    /**
     * Match an order (Admin only)
     */
    @Transactional
    public OrderDTO matchOrder(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId.toString()));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BusinessException(
                    String.format("Only PENDING orders can be matched. Current status: %s", order.getStatus()));
        }

        order.setStatus(OrderStatus.MATCHED);
        order.setFilledSize(order.getSize()); // Full fill
        order.setMatchedAt(LocalDateTime.now());

        Order savedOrder = orderRepository.save(order);
        log.info("Order matched: {}", orderId);

        // Create outbox event for settlement
        createOutboxEvent(savedOrder, TOPIC_ORDER_MATCHED, "OrderMatchedEvent");

        return toDTO(savedOrder);
    }

    /**
     * Create outbox event for reliable messaging
     */
    private void createOutboxEvent(Order order, String topic, String eventType) {
        try {
            Map<String, Object> payload = Map.of(
                    "orderId", order.getId().toString(),
                    "customerId", order.getCustomerId().toString(),
                    "assetSymbol", order.getAssetSymbol(),
                    "orderSide", order.getOrderSide().name(),
                    "size", order.getSize().toString(),
                    "price", order.getPrice().toString(),
                    "status", order.getStatus().name(),
                    "timestamp", LocalDateTime.now().toString()
            );

            OutboxEvent event = OutboxEvent.builder()
                    .aggregateId(order.getId())
                    .aggregateType("Order")
                    .eventType(eventType)
                    .topic(topic)
                    .partitionKey(order.getCustomerId().toString())
                    .payload(objectMapper.writeValueAsString(payload))
                    .build();

            outboxEventRepository.save(event);
            log.debug("Outbox event created: {} for order: {}", eventType, order.getId());

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize outbox event payload", e);
            throw new RuntimeException("Failed to create outbox event", e);
        }
    }

    private Pageable createPageable(OrderFilterRequest filter) {
        Sort.Direction direction = "ASC".equalsIgnoreCase(filter.getSortDirection())
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        return PageRequest.of(
                filter.getPage(),
                filter.getSize(),
                Sort.by(direction, filter.getSortBy())
        );
    }

    private OrderDTO toDTO(Order order) {
        return OrderDTO.builder()
                .id(order.getId())
                .customerId(order.getCustomerId())
                .assetSymbol(order.getAssetSymbol())
                .orderSide(order.getOrderSide())
                .orderType(order.getOrderType())
                .size(order.getSize())
                .price(order.getPrice())
                .filledSize(order.getFilledSize())
                .remainingSize(order.getRemainingSize())
                .totalValue(order.getTotalValue())
                .status(order.getStatus())
                .rejectionReason(order.getRejectionReason())
                .customerTierPriority(order.getCustomerTierPriority())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .matchedAt(order.getMatchedAt())
                .cancelledAt(order.getCancelledAt())
                .build();
    }
}
