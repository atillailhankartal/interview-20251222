package com.brokage.asset.repository;

import com.brokage.asset.entity.CustomerAsset;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerAssetRepository extends JpaRepository<CustomerAsset, UUID> {

    List<CustomerAsset> findByCustomerId(UUID customerId);

    Page<CustomerAsset> findByCustomerId(UUID customerId, Pageable pageable);

    Optional<CustomerAsset> findByCustomerIdAndAssetName(UUID customerId, String assetName);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ca FROM CustomerAsset ca WHERE ca.customerId = :customerId AND ca.assetName = :assetName")
    Optional<CustomerAsset> findByCustomerIdAndAssetNameForUpdate(
            @Param("customerId") UUID customerId,
            @Param("assetName") String assetName
    );

    @Query("SELECT ca FROM CustomerAsset ca WHERE ca.customerId = :customerId AND ca.usableSize > 0")
    List<CustomerAsset> findNonZeroAssetsByCustomerId(@Param("customerId") UUID customerId);

    boolean existsByCustomerIdAndAssetName(UUID customerId, String assetName);

    @Query("SELECT COALESCE(SUM(ca.usableSize), 0) FROM CustomerAsset ca " +
           "WHERE ca.customerId = :customerId AND ca.assetName = :assetName")
    java.math.BigDecimal getUsableBalance(
            @Param("customerId") UUID customerId,
            @Param("assetName") String assetName
    );

    /**
     * Get all TRY assets for admin dashboard (all customers' TRY balances)
     */
    @Query("SELECT ca FROM CustomerAsset ca WHERE ca.assetName = 'TRY'")
    List<CustomerAsset> findAllTryAssets();
}
