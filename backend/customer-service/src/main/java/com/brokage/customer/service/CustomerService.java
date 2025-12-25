package com.brokage.customer.service;

import com.brokage.common.dto.PageResponse;
import com.brokage.common.enums.CustomerStatus;
import com.brokage.common.enums.CustomerTier;
import com.brokage.common.exception.BusinessException;
import com.brokage.common.exception.ResourceNotFoundException;
import com.brokage.customer.dto.CreateCustomerRequest;
import com.brokage.customer.dto.CustomerDTO;
import com.brokage.customer.dto.CustomerFilterRequest;
import com.brokage.customer.dto.CustomerStatsDTO;
import com.brokage.customer.dto.UpdateCustomerRequest;
import com.brokage.customer.entity.Customer;
import com.brokage.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerService {

    private final CustomerRepository customerRepository;

    @Transactional
    public CustomerDTO createCustomer(CreateCustomerRequest request) {
        log.info("Creating customer with email: {}", request.getEmail());

        // Check for duplicate email
        if (customerRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Customer with email " + request.getEmail() + " already exists");
        }

        // Check for duplicate identity number
        if (customerRepository.existsByIdentityNumber(request.getIdentityNumber())) {
            throw new BusinessException("Customer with identity number already exists");
        }

        Customer customer = Customer.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .identityNumber(request.getIdentityNumber())
                .birthDate(request.getBirthDate())
                .tier(request.getTier() != null ? request.getTier() : CustomerTier.STANDARD)
                .status(CustomerStatus.ACTIVE)
                .build();

        Customer savedCustomer = customerRepository.save(customer);
        log.info("Customer created with ID: {}", savedCustomer.getId());

        return toDTO(savedCustomer);
    }

    @Transactional(readOnly = true)
    public CustomerDTO getCustomerById(UUID customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", customerId.toString()));
        return toDTO(customer);
    }

    @Transactional(readOnly = true)
    public CustomerDTO getCustomerByEmail(String email) {
        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Customer with email " + email + " not found"));
        return toDTO(customer);
    }

    @Transactional(readOnly = true)
    public CustomerDTO getCustomerByKeycloakUserId(String keycloakUserId) {
        Customer customer = customerRepository.findByKeycloakUserId(keycloakUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer with Keycloak ID " + keycloakUserId + " not found"));
        return toDTO(customer);
    }

    @Transactional
    public CustomerDTO updateCustomer(UUID customerId, UpdateCustomerRequest request) {
        log.info("Updating customer: {}", customerId);

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", customerId.toString()));

        // Check email uniqueness if changed
        if (request.getEmail() != null && !request.getEmail().equals(customer.getEmail())) {
            if (customerRepository.existsByEmail(request.getEmail())) {
                throw new BusinessException("Customer with email " + request.getEmail() + " already exists");
            }
            customer.setEmail(request.getEmail());
        }

        if (request.getFirstName() != null) {
            customer.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            customer.setLastName(request.getLastName());
        }
        if (request.getPhoneNumber() != null) {
            customer.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getBirthDate() != null) {
            customer.setBirthDate(request.getBirthDate());
        }

        Customer savedCustomer = customerRepository.save(customer);
        log.info("Customer updated: {}", customerId);

        return toDTO(savedCustomer);
    }

    @Transactional
    public CustomerDTO updateCustomerTier(UUID customerId, CustomerTier newTier) {
        log.info("Updating customer {} tier to {}", customerId, newTier);

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", customerId.toString()));

        customer.setTier(newTier);
        Customer savedCustomer = customerRepository.save(customer);

        log.info("Customer tier updated: {} -> {}", customerId, newTier);
        return toDTO(savedCustomer);
    }

    @Transactional
    public CustomerDTO updateCustomerStatus(UUID customerId, CustomerStatus newStatus) {
        log.info("Updating customer {} status to {}", customerId, newStatus);

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", customerId.toString()));

        customer.setStatus(newStatus);
        Customer savedCustomer = customerRepository.save(customer);

        log.info("Customer status updated: {} -> {}", customerId, newStatus);
        return toDTO(savedCustomer);
    }

    @Transactional
    public CustomerDTO linkKeycloakUser(UUID customerId, String keycloakUserId) {
        log.info("Linking customer {} to Keycloak user {}", customerId, keycloakUserId);

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", customerId.toString()));

        customer.setKeycloakUserId(keycloakUserId);
        Customer savedCustomer = customerRepository.save(customer);

        return toDTO(savedCustomer);
    }

    @Transactional(readOnly = true)
    public PageResponse<CustomerDTO> listCustomers(CustomerFilterRequest filter) {
        Pageable pageable = PageRequest.of(
                filter.getPage(),
                filter.getSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<Customer> customerPage;

        if (filter.isOrderableOnly()) {
            // Only return customers that can have orders (role = CUSTOMER)
            customerPage = customerRepository.findOrderableCustomers(
                    filter.getTier(),
                    filter.getStatus(),
                    filter.getSearch(),
                    pageable
            );
        } else {
            customerPage = customerRepository.findAllByFilters(
                    filter.getTier(),
                    filter.getStatus(),
                    filter.getRole(),
                    filter.getSearch(),
                    pageable
            );
        }

        return PageResponse.<CustomerDTO>builder()
                .content(customerPage.getContent().stream().map(this::toDTO).toList())
                .pageNumber(customerPage.getNumber())
                .pageSize(customerPage.getSize())
                .totalElements(customerPage.getTotalElements())
                .totalPages(customerPage.getTotalPages())
                .first(customerPage.isFirst())
                .last(customerPage.isLast())
                .build();
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

    /**
     * Get customer statistics for dashboard
     */
    @Transactional(readOnly = true)
    public CustomerStatsDTO getStats() {
        long totalCustomers = customerRepository.countCustomers();
        long totalBrokers = customerRepository.countBrokers();
        long totalAdmins = customerRepository.countAdmins();
        long activeUsers = customerRepository.countActiveUsers();
        long total = customerRepository.count();

        return CustomerStatsDTO.builder()
                .totalCustomers(totalCustomers)
                .totalBrokers(totalBrokers)
                .totalAdmins(totalAdmins)
                .activeUsers(activeUsers)
                .inactiveUsers(total - activeUsers)
                .build();
    }
}
