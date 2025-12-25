package com.brokage.order.service;

import com.brokage.common.enums.OrderStatus;
import com.brokage.common.enums.OrderType;
import com.brokage.common.exception.BusinessException;
import com.brokage.common.exception.ResourceNotFoundException;
import com.brokage.order.client.CustomerClient;
import com.brokage.order.context.RequestContext;
import com.brokage.order.dto.CreateOrderRequest;
import com.brokage.order.dto.OrderDTO;
import com.brokage.order.dto.OrderFilterRequest;
import com.brokage.order.dto.OrderStatsDTO;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final CustomerClient customerClient;

    private static final String TOPIC_ORDER_CREATED = "order.created";
    private static final String TOPIC_ORDER_CANCELLED = "order.cancelled";
    private static final String TOPIC_ORDER_MATCHED = "order.matched";

    /**
     * Create a new order with idempotency support
     */
    @Transactional
    public OrderDTO createOrder(CreateOrderRequest request) {
        log.info("Creating order for customer: {}, asset: {}, side: {}",
                request.getCustomerId(), request.getAssetName(), request.getOrderSide());

        // Validate customer can have orders
        if (!customerClient.isCustomerOrderable(request.getCustomerId())) {
            String role = customerClient.getCustomerRole(request.getCustomerId());
            log.warn("Customer {} with role {} cannot have orders created for them",
                    request.getCustomerId(), role);
            throw new BusinessException(
                    "Orders cannot be created for this customer. " +
                    "Only customers with CUSTOMER role can have orders.");
        }

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
                .assetName(request.getAssetName().toUpperCase())
                .orderSide(request.getOrderSide())
                .orderType(request.getOrderType() != null ? request.getOrderType() : OrderType.LIMIT)
                .size(request.getSize())
                .price(request.getPrice())
                .status(OrderStatus.PENDING)
                .idempotencyKey(request.getIdempotencyKey())
                .build();

        Order savedOrder = orderRepository.saveAndFlush(order);
        log.info("Order created with ID: {}, createdAt: {}", savedOrder.getId(), savedOrder.getCreatedAt());

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
                filter.getAssetName(),
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
                filter.getAssetName(),
                filter.getStatus(),
                filter.getOrderSide(),
                filter.getStartDate(),
                filter.getEndDate(),
                pageable
        );

        return orders.map(this::toDTO);
    }

    /**
     * List orders for a broker's customers
     * Fetches customer IDs from customer service and filters orders
     */
    @Transactional(readOnly = true)
    public Page<OrderDTO> listOrdersForBroker(UUID brokerId, OrderFilterRequest filter) {
        log.debug("Listing orders for broker: {}", brokerId);
        Pageable pageable = createPageable(filter);

        // Get broker's customer IDs from customer service
        List<UUID> brokerCustomerIds = customerClient.getBrokerCustomerIds(brokerId);

        if (brokerCustomerIds.isEmpty()) {
            log.debug("No customers found for broker: {}", brokerId);
            return Page.empty(pageable);
        }

        Page<Order> orders = orderRepository.findByCustomerIdsAndFilters(
                brokerCustomerIds,
                filter.getAssetName(),
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
     * Update order status from saga events
     */
    @Transactional
    public void updateOrderStatus(UUID orderId, OrderStatus newStatus, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    log.warn("Order not found for status update: {}", orderId);
                    return new ResourceNotFoundException("Order", orderId.toString());
                });

        OrderStatus oldStatus = order.getStatus();

        // Allow status transitions
        if (oldStatus == OrderStatus.CANCELLED || oldStatus == OrderStatus.MATCHED ||
            oldStatus == OrderStatus.FILLED) {
            log.warn("Cannot update order {} status from {} - order is in terminal state", orderId, oldStatus);
            return;
        }

        order.setStatus(newStatus);

        if (newStatus == OrderStatus.REJECTED && reason != null) {
            order.setRejectionReason(reason);
        }

        if (newStatus == OrderStatus.MATCHED) {
            order.setFilledSize(order.getSize());
            order.setMatchedAt(LocalDateTime.now());
        }

        orderRepository.save(order);
        log.info("Order {} status updated: {} -> {}", orderId, oldStatus, newStatus);
    }

    /**
     * Create outbox event for reliable messaging
     */
    private void createOutboxEvent(Order order, String topic, String eventType) {
        try {
            Map<String, Object> payload = Map.ofEntries(
                    Map.entry("eventType", eventType),
                    Map.entry("orderId", order.getId().toString()),
                    Map.entry("customerId", order.getCustomerId().toString()),
                    Map.entry("assetName", order.getAssetName()),
                    Map.entry("orderSide", order.getOrderSide().name()),
                    Map.entry("size", order.getSize().toString()),
                    Map.entry("price", order.getPrice().toString()),
                    Map.entry("status", order.getStatus().name()),
                    Map.entry("totalValue", order.getTotalValue().toString()),
                    Map.entry("timestamp", LocalDateTime.now().toString())
            );

            OutboxEvent.OutboxEventBuilder eventBuilder = OutboxEvent.builder()
                    .aggregateId(order.getId())
                    .aggregateType("Order")
                    .eventType(eventType)
                    .topic(topic)
                    .partitionKey(order.getCustomerId().toString())
                    .payload(objectMapper.writeValueAsString(payload));

            // Enrich with request context for audit
            RequestContext ctx = RequestContext.get();
            if (ctx != null) {
                eventBuilder
                        .traceId(ctx.getTraceId())
                        .spanId(ctx.getSpanId())
                        .ipAddress(ctx.getIpAddress())
                        .userAgent(ctx.getUserAgent())
                        .requestId(ctx.getRequestId())
                        .performedBy(ctx.getPerformedBy())
                        .performedByRole(ctx.getPerformedByRole());
            }

            OutboxEvent event = eventBuilder.build();

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
                .assetName(order.getAssetName())
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

    /**
     * Get order statistics for dashboard
     */
    @Transactional(readOnly = true)
    public OrderStatsDTO getOrderStats() {
        long pendingOrders = orderRepository.countByStatus(OrderStatus.PENDING);
        long matchedOrders = orderRepository.countByStatus(OrderStatus.MATCHED);
        long cancelledOrders = orderRepository.countByStatus(OrderStatus.CANCELLED);
        long totalOrders = orderRepository.countAllOrders();
        BigDecimal totalVolume = orderRepository.calculateTotalTradingVolume();

        List<Order> recentOrders = orderRepository.findRecentOrders(PageRequest.of(0, 10));
        List<OrderDTO> recentOrderDTOs = recentOrders.stream().map(this::toDTO).toList();

        return OrderStatsDTO.builder()
                .pendingOrders(pendingOrders)
                .matchedOrders(matchedOrders)
                .cancelledOrders(cancelledOrders)
                .totalOrders(totalOrders)
                .totalVolume(totalVolume != null ? totalVolume : BigDecimal.ZERO)
                .recentOrders(recentOrderDTOs)
                .build();
    }
}
