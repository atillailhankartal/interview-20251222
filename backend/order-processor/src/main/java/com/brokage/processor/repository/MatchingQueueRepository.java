package com.brokage.processor.repository;

import com.brokage.common.enums.OrderSide;
import com.brokage.processor.entity.MatchingQueue;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MatchingQueueRepository extends JpaRepository<MatchingQueue, UUID> {

    Optional<MatchingQueue> findByOrderId(UUID orderId);

    @Query("SELECT m FROM MatchingQueue m WHERE m.assetName = :assetName " +
           "AND m.orderSide = :orderSide AND m.status IN ('ACTIVE', 'PARTIALLY_MATCHED') " +
           "ORDER BY m.tierPriority DESC, " +
           "CASE WHEN :orderSide = 'BUY' THEN m.price END DESC, " +
           "CASE WHEN :orderSide = 'SELL' THEN m.price END ASC, " +
           "m.queuedAt ASC")
    List<MatchingQueue> findMatchingOrders(
            @Param("assetName") String assetName,
            @Param("orderSide") OrderSide orderSide);

    @Query("SELECT m FROM MatchingQueue m WHERE m.assetName = :assetName " +
           "AND m.orderSide = 'SELL' AND m.status IN ('ACTIVE', 'PARTIALLY_MATCHED') " +
           "AND m.price <= :maxPrice " +
           "ORDER BY m.tierPriority DESC, m.price ASC, m.queuedAt ASC")
    List<MatchingQueue> findSellOrdersForBuy(
            @Param("assetName") String assetName,
            @Param("maxPrice") BigDecimal maxPrice);

    @Query("SELECT m FROM MatchingQueue m WHERE m.assetName = :assetName " +
           "AND m.orderSide = 'BUY' AND m.status IN ('ACTIVE', 'PARTIALLY_MATCHED') " +
           "AND m.price >= :minPrice " +
           "ORDER BY m.tierPriority DESC, m.price DESC, m.queuedAt ASC")
    List<MatchingQueue> findBuyOrdersForSell(
            @Param("assetName") String assetName,
            @Param("minPrice") BigDecimal minPrice);

    Page<MatchingQueue> findByAssetNameAndStatusIn(
            String assetName,
            List<MatchingQueue.QueueStatus> statuses,
            Pageable pageable);

    Page<MatchingQueue> findByCustomerId(UUID customerId, Pageable pageable);

    @Query("SELECT COUNT(m) FROM MatchingQueue m WHERE m.status IN ('ACTIVE', 'PARTIALLY_MATCHED')")
    long countActiveOrders();

    @Query("SELECT COUNT(m) FROM MatchingQueue m WHERE m.assetName = :assetName " +
           "AND m.status IN ('ACTIVE', 'PARTIALLY_MATCHED')")
    long countActiveOrdersByAsset(@Param("assetName") String assetName);

    boolean existsByOrderId(UUID orderId);
}
