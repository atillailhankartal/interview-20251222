package com.brokage.customer.repository;

import com.brokage.common.enums.CustomerRole;
import com.brokage.common.enums.CustomerStatus;
import com.brokage.common.enums.CustomerTier;
import com.brokage.customer.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    Optional<Customer> findByEmail(String email);

    Optional<Customer> findByIdentityNumber(String identityNumber);

    Optional<Customer> findByKeycloakUserId(String keycloakUserId);

    boolean existsByEmail(String email);

    boolean existsByIdentityNumber(String identityNumber);

    @Query("SELECT c FROM Customer c WHERE " +
            "(CAST(:tier AS string) IS NULL OR c.tier = :tier) AND " +
            "(CAST(:status AS string) IS NULL OR c.status = :status) AND " +
            "(CAST(:role AS string) IS NULL OR c.role = :role) AND " +
            "(:search IS NULL OR :search = '' OR " +
            "LOWER(c.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(c.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(c.email) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Customer> findAllByFilters(
            @Param("tier") CustomerTier tier,
            @Param("status") CustomerStatus status,
            @Param("role") CustomerRole role,
            @Param("search") String search,
            Pageable pageable
    );

    /**
     * Find only orderable customers (CUSTOMER role only)
     */
    @Query("SELECT c FROM Customer c WHERE c.role = 'CUSTOMER' AND " +
            "(CAST(:tier AS string) IS NULL OR c.tier = :tier) AND " +
            "(CAST(:status AS string) IS NULL OR c.status = :status) AND " +
            "(:search IS NULL OR :search = '' OR " +
            "LOWER(c.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(c.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(c.email) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Customer> findOrderableCustomers(
            @Param("tier") CustomerTier tier,
            @Param("status") CustomerStatus status,
            @Param("search") String search,
            Pageable pageable
    );

    // Statistics queries
    long countByRole(CustomerRole role);

    long countByStatus(CustomerStatus status);

    @Query("SELECT COUNT(c) FROM Customer c WHERE c.role = 'CUSTOMER'")
    long countCustomers();

    @Query("SELECT COUNT(c) FROM Customer c WHERE c.role = 'BROKER'")
    long countBrokers();

    @Query("SELECT COUNT(c) FROM Customer c WHERE c.role = 'ADMIN'")
    long countAdmins();

    @Query("SELECT COUNT(c) FROM Customer c WHERE c.status = 'ACTIVE'")
    long countActiveUsers();
}
