package com.brokage.processor.entity;

import com.brokage.common.entity.BaseEntity;
import com.brokage.common.enums.OrderSide;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "trades", indexes = {
        @Index(name = "idx_trade_buy_order", columnList = "buy_order_id"),
        @Index(name = "idx_trade_sell_order", columnList = "sell_order_id"),
        @Index(name = "idx_trade_asset", columnList = "asset_name"),
        @Index(name = "idx_trade_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Trade extends BaseEntity {

    @Column(name = "buy_order_id", nullable = false)
    private UUID buyOrderId;

    @Column(name = "sell_order_id", nullable = false)
    private UUID sellOrderId;

    @Column(name = "buyer_customer_id", nullable = false)
    private UUID buyerCustomerId;

    @Column(name = "seller_customer_id", nullable = false)
    private UUID sellerCustomerId;

    @Column(name = "asset_name", nullable = false, length = 20)
    private String assetName;

    @Column(name = "quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity;

    @Column(name = "price", nullable = false, precision = 19, scale = 4)
    private BigDecimal price;

    @Column(name = "total_value", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "taker_side", nullable = false)
    private OrderSide takerSide;

    public static Trade create(UUID buyOrderId, UUID sellOrderId,
                               UUID buyerCustomerId, UUID sellerCustomerId,
                               String assetName, BigDecimal quantity,
                               BigDecimal price, OrderSide takerSide) {
        return Trade.builder()
                .buyOrderId(buyOrderId)
                .sellOrderId(sellOrderId)
                .buyerCustomerId(buyerCustomerId)
                .sellerCustomerId(sellerCustomerId)
                .assetName(assetName)
                .quantity(quantity)
                .price(price)
                .totalValue(quantity.multiply(price))
                .takerSide(takerSide)
                .build();
    }
}
