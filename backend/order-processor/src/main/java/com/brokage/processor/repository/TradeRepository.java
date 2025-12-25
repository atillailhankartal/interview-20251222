package com.brokage.processor.repository;

import com.brokage.processor.entity.Trade;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface TradeRepository extends JpaRepository<Trade, UUID> {

    Page<Trade> findByBuyerCustomerIdOrSellerCustomerId(
            UUID buyerCustomerId, UUID sellerCustomerId, Pageable pageable);

    Page<Trade> findByAssetName(String assetName, Pageable pageable);

    List<Trade> findByBuyOrderIdOrSellOrderId(UUID buyOrderId, UUID sellOrderId);

    @Query("SELECT t FROM Trade t WHERE t.buyerCustomerId = :customerId OR t.sellerCustomerId = :customerId")
    Page<Trade> findByCustomerId(@Param("customerId") UUID customerId, Pageable pageable);

    @Query("SELECT t FROM Trade t WHERE t.createdAt BETWEEN :startDate AND :endDate")
    Page<Trade> findByCreatedAtBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    @Query("SELECT t FROM Trade t WHERE t.assetName = :assetName AND t.createdAt BETWEEN :startDate AND :endDate")
    Page<Trade> findByAssetNameAndCreatedAtBetween(
            @Param("assetName") String assetName,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    @Query("SELECT COUNT(t) FROM Trade t WHERE t.assetName = :assetName")
    long countByAssetName(@Param("assetName") String assetName);

    @Query("SELECT SUM(t.totalValue) FROM Trade t WHERE t.assetName = :assetName AND t.createdAt >= :since")
    java.math.BigDecimal getTotalVolumeByAssetNameSince(
            @Param("assetName") String assetName,
            @Param("since") LocalDateTime since);
}
