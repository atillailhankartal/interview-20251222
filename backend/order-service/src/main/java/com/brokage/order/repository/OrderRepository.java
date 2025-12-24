package com.brokage.order.repository;

import com.brokage.common.enums.OrderSide;
import com.brokage.common.enums.OrderStatus;
import com.brokage.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID>, JpaSpecificationExecutor<Order> {

    Optional<Order> findByIdempotencyKey(String idempotencyKey);

    List<Order> findByCustomerId(UUID customerId);

    Page<Order> findByCustomerId(UUID customerId, Pageable pageable);

    List<Order> findByCustomerIdAndStatus(UUID customerId, OrderStatus status);

    @Query("SELECT o FROM Order o WHERE o.customerId = :customerId " +
           "AND (COALESCE(:assetSymbol, '') = '' OR o.assetSymbol = :assetSymbol) " +
           "AND (:status IS NULL OR o.status = :status) " +
           "AND (:orderSide IS NULL OR o.orderSide = :orderSide) " +
           "AND (CAST(:startDate AS timestamp) IS NULL OR o.createdAt >= :startDate) " +
           "AND (CAST(:endDate AS timestamp) IS NULL OR o.createdAt <= :endDate)")
    Page<Order> findByFilters(
            @Param("customerId") UUID customerId,
            @Param("assetSymbol") String assetSymbol,
            @Param("status") OrderStatus status,
            @Param("orderSide") OrderSide orderSide,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    @Query("SELECT o FROM Order o WHERE " +
           "(CAST(:customerId AS uuid) IS NULL OR o.customerId = :customerId) " +
           "AND (COALESCE(:assetSymbol, '') = '' OR o.assetSymbol = :assetSymbol) " +
           "AND (:status IS NULL OR o.status = :status) " +
           "AND (:orderSide IS NULL OR o.orderSide = :orderSide) " +
           "AND (CAST(:startDate AS timestamp) IS NULL OR o.createdAt >= :startDate) " +
           "AND (CAST(:endDate AS timestamp) IS NULL OR o.createdAt <= :endDate)")
    Page<Order> findAllByFilters(
            @Param("customerId") UUID customerId,
            @Param("assetSymbol") String assetSymbol,
            @Param("status") OrderStatus status,
            @Param("orderSide") OrderSide orderSide,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    // For order matching - find pending orders sorted by tier priority, price, and time
    @Query("SELECT o FROM Order o WHERE o.assetSymbol = :assetSymbol " +
           "AND o.orderSide = :orderSide " +
           "AND o.status = 'PENDING' " +
           "ORDER BY o.customerTierPriority ASC, o.price DESC, o.createdAt ASC")
    List<Order> findMatchableOrders(
            @Param("assetSymbol") String assetSymbol,
            @Param("orderSide") OrderSide orderSide
    );

    boolean existsByIdempotencyKey(String idempotencyKey);
}
