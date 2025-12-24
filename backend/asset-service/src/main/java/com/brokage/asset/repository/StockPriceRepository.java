package com.brokage.asset.repository;

import com.brokage.asset.entity.StockPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockPriceRepository extends JpaRepository<StockPrice, String> {

    List<StockPrice> findByExchange(String exchange);

    List<StockPrice> findAllByOrderBySymbol();

    List<StockPrice> findByExchangeOrderBySymbol(String exchange);
}
