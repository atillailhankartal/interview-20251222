package com.brokage.customer.entity;

import com.brokage.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "broker_customers",
       uniqueConstraints = @UniqueConstraint(columnNames = {"broker_id", "customer_id"}),
       indexes = {
        @Index(name = "idx_broker_customers_broker", columnList = "broker_id"),
        @Index(name = "idx_broker_customers_customer", columnList = "customer_id"),
        @Index(name = "idx_broker_customers_active", columnList = "active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BrokerCustomer extends BaseEntity {

    @Column(name = "broker_id", nullable = false)
    private UUID brokerId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "notes", length = 1000)
    private String notes;
}
