package com.brokage.customer.repository;

import com.brokage.customer.entity.BrokerCustomer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BrokerCustomerRepository extends JpaRepository<BrokerCustomer, UUID> {

    /**
     * Check if a broker-customer relationship exists and is active
     */
    boolean existsByBrokerIdAndCustomerIdAndActiveTrue(UUID brokerId, UUID customerId);

    /**
     * Find all customers assigned to a broker
     */
    List<BrokerCustomer> findByBrokerIdAndActiveTrue(UUID brokerId);

    /**
     * Find all customers assigned to a broker (paged)
     */
    Page<BrokerCustomer> findByBrokerIdAndActiveTrue(UUID brokerId, Pageable pageable);

    /**
     * Find a specific broker-customer relationship
     */
    Optional<BrokerCustomer> findByBrokerIdAndCustomerId(UUID brokerId, UUID customerId);

    /**
     * Get all customer IDs for a broker
     */
    @Query("SELECT bc.customerId FROM BrokerCustomer bc WHERE bc.brokerId = :brokerId AND bc.active = true")
    List<UUID> findCustomerIdsByBrokerId(@Param("brokerId") UUID brokerId);

    /**
     * Count active customers for a broker
     */
    long countByBrokerIdAndActiveTrue(UUID brokerId);

    /**
     * Find all brokers assigned to a customer
     */
    List<BrokerCustomer> findByCustomerIdAndActiveTrue(UUID customerId);

    /**
     * Find all brokers assigned to a customer (paged)
     */
    Page<BrokerCustomer> findByCustomerIdAndActiveTrue(UUID customerId, Pageable pageable);

    /**
     * Get all broker IDs for a customer
     */
    @Query("SELECT bc.brokerId FROM BrokerCustomer bc WHERE bc.customerId = :customerId AND bc.active = true")
    List<UUID> findBrokerIdsByCustomerId(@Param("customerId") UUID customerId);
}
