package com.brokage.asset.repository;

import com.brokage.asset.entity.Asset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssetRepository extends JpaRepository<Asset, Long> {

    Optional<Asset> findBySymbol(String symbol);

    List<Asset> findByActiveTrue();

    List<Asset> findByActiveTrueOrderBySymbol();

    boolean existsBySymbol(String symbol);
}
