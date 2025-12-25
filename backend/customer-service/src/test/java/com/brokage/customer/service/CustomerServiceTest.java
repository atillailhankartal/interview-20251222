package com.brokage.customer.service;

import com.brokage.common.enums.CustomerStatus;
import com.brokage.common.enums.CustomerTier;
import com.brokage.common.exception.BusinessException;
import com.brokage.common.exception.ResourceNotFoundException;
import com.brokage.customer.dto.CreateCustomerRequest;
import com.brokage.customer.dto.CustomerDTO;
import com.brokage.customer.dto.CustomerFilterRequest;
import com.brokage.customer.dto.UpdateCustomerRequest;
import com.brokage.customer.entity.Customer;
import com.brokage.customer.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerService customerService;

    private UUID customerId;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("Create Customer Tests")
    class CreateCustomerTests {

        @Test
        @DisplayName("Should create customer successfully")
        void shouldCreateCustomerSuccessfully() {
            // Given
            CreateCustomerRequest request = CreateCustomerRequest.builder()
                    .firstName("John")
                    .lastName("Doe")
                    .email("john.doe@example.com")
                    .phoneNumber("+905551234567")
                    .identityNumber("12345678901")
                    .birthDate(LocalDate.of(1990, 1, 15))
                    .tier(CustomerTier.STANDARD)
                    .build();

            Customer savedCustomer = createCustomer(customerId, "John", "Doe",
                    "john.doe@example.com", CustomerTier.STANDARD);

            when(customerRepository.existsByEmail(request.getEmail())).thenReturn(false);
            when(customerRepository.existsByIdentityNumber(request.getIdentityNumber())).thenReturn(false);
            when(customerRepository.save(any(Customer.class))).thenReturn(savedCustomer);

            // When
            CustomerDTO result = customerService.createCustomer(request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getFirstName()).isEqualTo("John");
            assertThat(result.getLastName()).isEqualTo("Doe");
            assertThat(result.getEmail()).isEqualTo("john.doe@example.com");
            assertThat(result.getTier()).isEqualTo(CustomerTier.STANDARD);
            verify(customerRepository).save(any(Customer.class));
        }

        @Test
        @DisplayName("Should throw exception for duplicate email")
        void shouldThrowExceptionForDuplicateEmail() {
            // Given
            CreateCustomerRequest request = CreateCustomerRequest.builder()
                    .firstName("John")
                    .lastName("Doe")
                    .email("existing@example.com")
                    .identityNumber("12345678901")
                    .build();

            when(customerRepository.existsByEmail(request.getEmail())).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> customerService.createCustomer(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("already exists");
        }

        @Test
        @DisplayName("Should throw exception for duplicate identity number")
        void shouldThrowExceptionForDuplicateIdentityNumber() {
            // Given
            CreateCustomerRequest request = CreateCustomerRequest.builder()
                    .firstName("John")
                    .lastName("Doe")
                    .email("john@example.com")
                    .identityNumber("12345678901")
                    .build();

            when(customerRepository.existsByEmail(request.getEmail())).thenReturn(false);
            when(customerRepository.existsByIdentityNumber(request.getIdentityNumber())).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> customerService.createCustomer(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("identity number");
        }

        @Test
        @DisplayName("Should create customer with default STANDARD tier")
        void shouldCreateCustomerWithDefaultTier() {
            // Given
            CreateCustomerRequest request = CreateCustomerRequest.builder()
                    .firstName("Jane")
                    .lastName("Doe")
                    .email("jane.doe@example.com")
                    .identityNumber("98765432109")
                    .tier(null)
                    .build();

            Customer savedCustomer = createCustomer(customerId, "Jane", "Doe",
                    "jane.doe@example.com", CustomerTier.STANDARD);

            when(customerRepository.existsByEmail(any())).thenReturn(false);
            when(customerRepository.existsByIdentityNumber(any())).thenReturn(false);
            when(customerRepository.save(any(Customer.class))).thenReturn(savedCustomer);

            // When
            CustomerDTO result = customerService.createCustomer(request);

            // Then
            assertThat(result.getTier()).isEqualTo(CustomerTier.STANDARD);
        }
    }

    @Nested
    @DisplayName("Get Customer Tests")
    class GetCustomerTests {

        @Test
        @DisplayName("Should get customer by ID")
        void shouldGetCustomerById() {
            // Given
            Customer customer = createCustomer(customerId, "John", "Doe",
                    "john@example.com", CustomerTier.PREMIUM);

            when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));

            // When
            CustomerDTO result = customerService.getCustomerById(customerId);

            // Then
            assertThat(result.getId()).isEqualTo(customerId);
            assertThat(result.getFirstName()).isEqualTo("John");
            assertThat(result.getTier()).isEqualTo(CustomerTier.PREMIUM);
        }

        @Test
        @DisplayName("Should throw exception when customer not found")
        void shouldThrowExceptionWhenCustomerNotFound() {
            // Given
            when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> customerService.getCustomerById(customerId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Should get customer by email")
        void shouldGetCustomerByEmail() {
            // Given
            String email = "john@example.com";
            Customer customer = createCustomer(customerId, "John", "Doe", email, CustomerTier.VIP);

            when(customerRepository.findByEmail(email)).thenReturn(Optional.of(customer));

            // When
            CustomerDTO result = customerService.getCustomerByEmail(email);

            // Then
            assertThat(result.getEmail()).isEqualTo(email);
            assertThat(result.getTier()).isEqualTo(CustomerTier.VIP);
        }

        @Test
        @DisplayName("Should get customer by Keycloak user ID")
        void shouldGetCustomerByKeycloakUserId() {
            // Given
            String keycloakUserId = "keycloak-user-123";
            Customer customer = createCustomer(customerId, "John", "Doe",
                    "john@example.com", CustomerTier.STANDARD);
            customer.setKeycloakUserId(keycloakUserId);

            when(customerRepository.findByKeycloakUserId(keycloakUserId)).thenReturn(Optional.of(customer));

            // When
            CustomerDTO result = customerService.getCustomerByKeycloakUserId(keycloakUserId);

            // Then
            assertThat(result.getId()).isEqualTo(customerId);
        }
    }

    @Nested
    @DisplayName("Update Customer Tests")
    class UpdateCustomerTests {

        @Test
        @DisplayName("Should update customer successfully")
        void shouldUpdateCustomerSuccessfully() {
            // Given
            Customer customer = createCustomer(customerId, "John", "Doe",
                    "john@example.com", CustomerTier.STANDARD);

            UpdateCustomerRequest request = UpdateCustomerRequest.builder()
                    .firstName("Johnny")
                    .phoneNumber("+905559876543")
                    .build();

            when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
            when(customerRepository.save(any(Customer.class))).thenReturn(customer);

            // When
            CustomerDTO result = customerService.updateCustomer(customerId, request);

            // Then
            assertThat(result).isNotNull();
            verify(customerRepository).save(any(Customer.class));
        }

        @Test
        @DisplayName("Should throw exception when updating email to existing one")
        void shouldThrowExceptionWhenUpdatingToExistingEmail() {
            // Given
            Customer customer = createCustomer(customerId, "John", "Doe",
                    "john@example.com", CustomerTier.STANDARD);

            UpdateCustomerRequest request = UpdateCustomerRequest.builder()
                    .email("existing@example.com")
                    .build();

            when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
            when(customerRepository.existsByEmail("existing@example.com")).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> customerService.updateCustomer(customerId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("already exists");
        }

        @Test
        @DisplayName("Should update customer tier")
        void shouldUpdateCustomerTier() {
            // Given
            Customer customer = createCustomer(customerId, "John", "Doe",
                    "john@example.com", CustomerTier.STANDARD);

            when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
            when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> {
                Customer c = inv.getArgument(0);
                c.setTier(CustomerTier.VIP);
                return c;
            });

            // When
            CustomerDTO result = customerService.updateCustomerTier(customerId, CustomerTier.VIP);

            // Then
            assertThat(result.getTier()).isEqualTo(CustomerTier.VIP);
        }

        @Test
        @DisplayName("Should update customer status")
        void shouldUpdateCustomerStatus() {
            // Given
            Customer customer = createCustomer(customerId, "John", "Doe",
                    "john@example.com", CustomerTier.STANDARD);

            when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
            when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> {
                Customer c = inv.getArgument(0);
                c.setStatus(CustomerStatus.SUSPENDED);
                return c;
            });

            // When
            CustomerDTO result = customerService.updateCustomerStatus(customerId, CustomerStatus.SUSPENDED);

            // Then
            assertThat(result.getStatus()).isEqualTo(CustomerStatus.SUSPENDED);
        }
    }

    @Nested
    @DisplayName("List Customers Tests")
    class ListCustomersTests {

        @Test
        @DisplayName("Should list customers with pagination")
        void shouldListCustomersWithPagination() {
            // Given
            CustomerFilterRequest filter = CustomerFilterRequest.builder()
                    .page(0)
                    .size(10)
                    .build();

            List<Customer> customers = List.of(
                    createCustomer(UUID.randomUUID(), "John", "Doe", "john@example.com", CustomerTier.STANDARD),
                    createCustomer(UUID.randomUUID(), "Jane", "Smith", "jane@example.com", CustomerTier.PREMIUM)
            );

            Page<Customer> customerPage = new PageImpl<>(customers);
            when(customerRepository.findAllByFilters(any(), any(), any(), any(), any(Pageable.class)))
                    .thenReturn(customerPage);

            // When
            var result = customerService.listCustomers(filter);

            // Then
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should filter customers by tier")
        void shouldFilterCustomersByTier() {
            // Given
            CustomerFilterRequest filter = CustomerFilterRequest.builder()
                    .tier(CustomerTier.VIP)
                    .page(0)
                    .size(10)
                    .build();

            List<Customer> vipCustomers = List.of(
                    createCustomer(UUID.randomUUID(), "VIP", "Customer", "vip@example.com", CustomerTier.VIP)
            );

            Page<Customer> customerPage = new PageImpl<>(vipCustomers);
            when(customerRepository.findAllByFilters(eq(CustomerTier.VIP), any(), any(), any(), any(Pageable.class)))
                    .thenReturn(customerPage);

            // When
            var result = customerService.listCustomers(filter);

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getTier()).isEqualTo(CustomerTier.VIP);
        }
    }

    @Nested
    @DisplayName("Link Keycloak User Tests")
    class LinkKeycloakUserTests {

        @Test
        @DisplayName("Should link Keycloak user successfully")
        void shouldLinkKeycloakUserSuccessfully() {
            // Given
            Customer customer = createCustomer(customerId, "John", "Doe",
                    "john@example.com", CustomerTier.STANDARD);
            String keycloakUserId = "keycloak-123";

            when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
            when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> {
                Customer c = inv.getArgument(0);
                c.setKeycloakUserId(keycloakUserId);
                return c;
            });

            // When
            CustomerDTO result = customerService.linkKeycloakUser(customerId, keycloakUserId);

            // Then
            assertThat(result).isNotNull();
            verify(customerRepository).save(any(Customer.class));
        }
    }

    private Customer createCustomer(UUID id, String firstName, String lastName,
                                     String email, CustomerTier tier) {
        Customer customer = Customer.builder()
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .identityNumber("12345678901")
                .tier(tier)
                .status(CustomerStatus.ACTIVE)
                .build();

        try {
            var idField = customer.getClass().getSuperclass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(customer, id);
        } catch (Exception e) {
            // Fallback
        }

        return customer;
    }
}
