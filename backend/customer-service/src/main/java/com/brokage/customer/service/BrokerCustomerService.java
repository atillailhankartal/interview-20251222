package com.brokage.customer.service;

import com.brokage.common.dto.PageResponse;
import com.brokage.common.enums.CustomerRole;
import com.brokage.common.exception.BusinessException;
import com.brokage.common.exception.ResourceNotFoundException;
import com.brokage.customer.dto.BrokerCustomerDTO;
import com.brokage.customer.dto.CustomerDTO;
import com.brokage.customer.entity.BrokerCustomer;
import com.brokage.customer.entity.Customer;
import com.brokage.customer.repository.BrokerCustomerRepository;
import com.brokage.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BrokerCustomerService {

    private final BrokerCustomerRepository brokerCustomerRepository;
    private final CustomerRepository customerRepository;

    /**
     * Check if a customer belongs to a broker
     */
    @Transactional(readOnly = true)
    public boolean isBrokerOfCustomer(UUID brokerId, UUID customerId) {
        // First verify the broker exists and has BROKER role
        Customer broker = customerRepository.findById(brokerId).orElse(null);
        if (broker == null || broker.getRole() != CustomerRole.BROKER) {
            return false;
        }

        return brokerCustomerRepository.existsByBrokerIdAndCustomerIdAndActiveTrue(brokerId, customerId);
    }

    /**
     * Get all customer IDs for a broker
     */
    @Transactional(readOnly = true)
    public List<UUID> getBrokerCustomerIds(UUID brokerId) {
        return brokerCustomerRepository.findCustomerIdsByBrokerId(brokerId);
    }

    /**
     * Get all customers for a broker (paged)
     */
    @Transactional(readOnly = true)
    public PageResponse<CustomerDTO> getBrokerCustomers(UUID brokerId, int page, int size) {
        log.debug("Getting customers for broker: {}, page: {}, size: {}", brokerId, page, size);

        // Verify broker exists
        Customer broker = customerRepository.findById(brokerId)
                .orElseThrow(() -> new ResourceNotFoundException("Broker", brokerId.toString()));

        if (broker.getRole() != CustomerRole.BROKER) {
            throw new BusinessException("User is not a broker");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<BrokerCustomer> brokerCustomerPage = brokerCustomerRepository.findByBrokerIdAndActiveTrue(brokerId, pageable);

        // Get customer details for each broker-customer relationship
        List<CustomerDTO> customers = brokerCustomerPage.getContent().stream()
                .map(bc -> customerRepository.findById(bc.getCustomerId()).orElse(null))
                .filter(c -> c != null)
                .map(this::toDTO)
                .collect(Collectors.toList());

        return PageResponse.<CustomerDTO>builder()
                .content(customers)
                .pageNumber(brokerCustomerPage.getNumber())
                .pageSize(brokerCustomerPage.getSize())
                .totalElements(brokerCustomerPage.getTotalElements())
                .totalPages(brokerCustomerPage.getTotalPages())
                .first(brokerCustomerPage.isFirst())
                .last(brokerCustomerPage.isLast())
                .build();
    }

    /**
     * Assign a customer to a broker
     */
    @Transactional
    public BrokerCustomerDTO assignCustomerToBroker(UUID brokerId, UUID customerId, String notes) {
        log.info("Assigning customer {} to broker {}", customerId, brokerId);

        // Verify broker exists and has BROKER role
        Customer broker = customerRepository.findById(brokerId)
                .orElseThrow(() -> new ResourceNotFoundException("Broker", brokerId.toString()));

        if (broker.getRole() != CustomerRole.BROKER) {
            throw new BusinessException("User is not a broker");
        }

        // Verify customer exists and has CUSTOMER role
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", customerId.toString()));

        if (customer.getRole() != CustomerRole.CUSTOMER) {
            throw new BusinessException("Can only assign customers with CUSTOMER role to brokers");
        }

        // Check if relationship already exists
        var existingRelation = brokerCustomerRepository.findByBrokerIdAndCustomerId(brokerId, customerId);
        if (existingRelation.isPresent()) {
            BrokerCustomer bc = existingRelation.get();
            if (bc.getActive()) {
                throw new BusinessException("Customer is already assigned to this broker");
            }
            // Reactivate the relationship
            bc.setActive(true);
            bc.setNotes(notes);
            BrokerCustomer saved = brokerCustomerRepository.save(bc);
            return toDTO(saved, customer);
        }

        // Create new relationship
        BrokerCustomer brokerCustomer = BrokerCustomer.builder()
                .brokerId(brokerId)
                .customerId(customerId)
                .active(true)
                .notes(notes)
                .build();

        BrokerCustomer saved = brokerCustomerRepository.save(brokerCustomer);
        log.info("Customer {} assigned to broker {}", customerId, brokerId);

        return toDTO(saved, customer);
    }

    /**
     * Get all brokers for a customer (paged)
     */
    @Transactional(readOnly = true)
    public PageResponse<CustomerDTO> getCustomerBrokers(UUID customerId, int page, int size) {
        log.debug("Getting brokers for customer: {}, page: {}, size: {}", customerId, page, size);

        // Verify customer exists
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", customerId.toString()));

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<BrokerCustomer> brokerCustomerPage = brokerCustomerRepository.findByCustomerIdAndActiveTrue(customerId, pageable);

        // Get broker details for each broker-customer relationship
        List<CustomerDTO> brokers = brokerCustomerPage.getContent().stream()
                .map(bc -> customerRepository.findById(bc.getBrokerId()).orElse(null))
                .filter(b -> b != null)
                .map(this::toDTO)
                .collect(Collectors.toList());

        return PageResponse.<CustomerDTO>builder()
                .content(brokers)
                .pageNumber(brokerCustomerPage.getNumber())
                .pageSize(brokerCustomerPage.getSize())
                .totalElements(brokerCustomerPage.getTotalElements())
                .totalPages(brokerCustomerPage.getTotalPages())
                .first(brokerCustomerPage.isFirst())
                .last(brokerCustomerPage.isLast())
                .build();
    }

    /**
     * Get all broker IDs for a customer
     */
    @Transactional(readOnly = true)
    public List<UUID> getCustomerBrokerIds(UUID customerId) {
        return brokerCustomerRepository.findBrokerIdsByCustomerId(customerId);
    }

    /**
     * Get all customers for a broker with optional search (for order creation)
     */
    @Transactional(readOnly = true)
    public PageResponse<CustomerDTO> getBrokerCustomersForOrder(UUID brokerId, String search, int page, int size) {
        log.debug("Getting orderable customers for broker: {}, search: {}, page: {}, size: {}", brokerId, search, page, size);

        // Verify broker exists
        Customer broker = customerRepository.findById(brokerId)
                .orElseThrow(() -> new ResourceNotFoundException("Broker", brokerId.toString()));

        if (broker.getRole() != CustomerRole.BROKER) {
            throw new BusinessException("User is not a broker");
        }

        // Get all customer IDs for this broker
        List<UUID> customerIds = brokerCustomerRepository.findCustomerIdsByBrokerId(brokerId);

        if (customerIds.isEmpty()) {
            return PageResponse.<CustomerDTO>builder()
                    .content(List.of())
                    .pageNumber(page)
                    .pageSize(size)
                    .totalElements(0)
                    .totalPages(0)
                    .first(true)
                    .last(true)
                    .build();
        }

        // Get customers with optional search filter
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "lastName", "firstName"));
        Page<Customer> customerPage;

        if (search != null && !search.isBlank()) {
            customerPage = customerRepository.findByIdInAndSearchAndOrderable(customerIds, search.toLowerCase(), pageable);
        } else {
            customerPage = customerRepository.findByIdInAndOrderableTrue(customerIds, pageable);
        }

        List<CustomerDTO> customers = customerPage.getContent().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        return PageResponse.<CustomerDTO>builder()
                .content(customers)
                .pageNumber(customerPage.getNumber())
                .pageSize(customerPage.getSize())
                .totalElements(customerPage.getTotalElements())
                .totalPages(customerPage.getTotalPages())
                .first(customerPage.isFirst())
                .last(customerPage.isLast())
                .build();
    }

    /**
     * Remove a customer from a broker
     */
    @Transactional
    public void removeCustomerFromBroker(UUID brokerId, UUID customerId) {
        log.info("Removing customer {} from broker {}", customerId, brokerId);

        BrokerCustomer brokerCustomer = brokerCustomerRepository.findByBrokerIdAndCustomerId(brokerId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Broker-Customer relationship not found"));

        brokerCustomer.setActive(false);
        brokerCustomerRepository.save(brokerCustomer);

        log.info("Customer {} removed from broker {}", customerId, brokerId);
    }

    private CustomerDTO toDTO(Customer customer) {
        return CustomerDTO.builder()
                .id(customer.getId())
                .firstName(customer.getFirstName())
                .lastName(customer.getLastName())
                .email(customer.getEmail())
                .phoneNumber(customer.getPhoneNumber())
                .identityNumber(customer.getIdentityNumber())
                .birthDate(customer.getBirthDate())
                .tier(customer.getTier())
                .status(customer.getStatus())
                .role(customer.getRole())
                .orderable(customer.isOrderable())
                .createdAt(customer.getCreatedAt())
                .updatedAt(customer.getUpdatedAt())
                .build();
    }

    private BrokerCustomerDTO toDTO(BrokerCustomer bc, Customer customer) {
        return BrokerCustomerDTO.builder()
                .id(bc.getId())
                .brokerId(bc.getBrokerId())
                .customerId(bc.getCustomerId())
                .customerName(customer.getFullName())
                .customerEmail(customer.getEmail())
                .active(bc.getActive())
                .notes(bc.getNotes())
                .createdAt(bc.getCreatedAt())
                .build();
    }
}
