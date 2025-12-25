package com.brokage.customer.controller;

import com.brokage.common.dto.PageResponse;
import com.brokage.common.enums.CustomerStatus;
import com.brokage.common.enums.CustomerTier;
import com.brokage.customer.config.TestSecurityConfig;
import com.brokage.customer.dto.CreateCustomerRequest;
import com.brokage.customer.dto.CustomerDTO;
import com.brokage.customer.dto.UpdateCustomerRequest;
import com.brokage.customer.service.BrokerCustomerService;
import com.brokage.customer.service.CustomerSecurityService;
import com.brokage.customer.service.CustomerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CustomerController.class)
@Import(TestSecurityConfig.class)
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CustomerService customerService;

    @MockBean
    private CustomerSecurityService customerSecurityService;

    @MockBean
    private BrokerCustomerService brokerCustomerService;

    private UUID customerId;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("Create Customer Tests")
    class CreateCustomerTests {

        @Test
        @DisplayName("Should create customer as ADMIN")
        void shouldCreateCustomerAsAdmin() throws Exception {
            // Given
            CreateCustomerRequest request = CreateCustomerRequest.builder()
                    .email("test@example.com")
                    .firstName("John")
                    .lastName("Doe")
                    .phoneNumber("+905551234567")
                    .identityNumber("12345678901")
                    .build();

            CustomerDTO customerDTO = createCustomerDTO(customerId, "test@example.com", "John", "Doe");

            when(customerService.createCustomer(any(CreateCustomerRequest.class))).thenReturn(customerDTO);

            // When & Then
            mockMvc.perform(post("/api/customers")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                    .jwt(jwt -> jwt
                                            .claim("realm_access", Map.of("roles", List.of("ADMIN")))
                                            .subject(UUID.randomUUID().toString())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.email").value("test@example.com"));

            verify(customerService).createCustomer(any(CreateCustomerRequest.class));
        }

        @Test
        @DisplayName("Should reject customer creation for non-ADMIN")
        void shouldRejectCustomerCreationForNonAdmin() throws Exception {
            // Given
            CreateCustomerRequest request = CreateCustomerRequest.builder()
                    .email("test@example.com")
                    .firstName("John")
                    .lastName("Doe")
                    .identityNumber("12345678901")
                    .build();

            // When & Then
            mockMvc.perform(post("/api/customers")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
                                    .jwt(jwt -> jwt
                                            .claim("realm_access", Map.of("roles", List.of("CUSTOMER")))
                                            .subject(customerId.toString())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());

            verify(customerService, never()).createCustomer(any());
        }
    }

    @Nested
    @DisplayName("Get Customer Tests")
    class GetCustomerTests {

        @Test
        @DisplayName("Should get customer by ID as ADMIN")
        void shouldGetCustomerByIdAsAdmin() throws Exception {
            // Given
            CustomerDTO customerDTO = createCustomerDTO(customerId, "test@example.com", "John", "Doe");

            when(customerService.getCustomerById(customerId)).thenReturn(customerDTO);

            // When & Then
            mockMvc.perform(get("/api/customers/{customerId}", customerId)
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                    .jwt(jwt -> jwt
                                            .claim("realm_access", Map.of("roles", List.of("ADMIN")))
                                            .subject(UUID.randomUUID().toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(customerId.toString()));

            verify(customerService).getCustomerById(customerId);
        }

        @Test
        @DisplayName("Should get own profile as CUSTOMER")
        void shouldGetOwnProfileAsCustomer() throws Exception {
            // Given
            CustomerDTO customerDTO = createCustomerDTO(customerId, "test@example.com", "John", "Doe");

            when(customerService.getCustomerByKeycloakUserId(customerId.toString())).thenReturn(customerDTO);

            // When & Then
            mockMvc.perform(get("/api/customers/me")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
                                    .jwt(jwt -> jwt
                                            .claim("realm_access", Map.of("roles", List.of("CUSTOMER")))
                                            .subject(customerId.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(customerService).getCustomerByKeycloakUserId(customerId.toString());
        }
    }

    @Nested
    @DisplayName("List Customers Tests")
    class ListCustomersTests {

        @Test
        @DisplayName("Should list customers as ADMIN")
        void shouldListCustomersAsAdmin() throws Exception {
            // Given
            List<CustomerDTO> customers = List.of(
                    createCustomerDTO(UUID.randomUUID(), "john@example.com", "John", "Doe"),
                    createCustomerDTO(UUID.randomUUID(), "jane@example.com", "Jane", "Doe")
            );
            Page<CustomerDTO> page = new PageImpl<>(customers);
            PageResponse<CustomerDTO> pageResponse = PageResponse.of(page);

            when(customerService.listCustomers(any())).thenReturn(pageResponse);

            // When & Then
            mockMvc.perform(get("/api/customers")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                    .jwt(jwt -> jwt
                                            .claim("realm_access", Map.of("roles", List.of("ADMIN")))
                                            .subject(UUID.randomUUID().toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isArray());

            verify(customerService).listCustomers(any());
        }

        @Test
        @DisplayName("Should deny listing customers for CUSTOMER")
        void shouldDenyListingCustomersForCustomer() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/customers")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
                                    .jwt(jwt -> jwt
                                            .claim("realm_access", Map.of("roles", List.of("CUSTOMER")))
                                            .subject(customerId.toString()))))
                    .andExpect(status().isForbidden());

            verify(customerService, never()).listCustomers(any());
        }
    }

    @Nested
    @DisplayName("Update Customer Tests")
    class UpdateCustomerTests {

        @Test
        @DisplayName("Should update customer as ADMIN")
        void shouldUpdateCustomerAsAdmin() throws Exception {
            // Given
            UpdateCustomerRequest request = UpdateCustomerRequest.builder()
                    .firstName("Johnny")
                    .lastName("Doe Updated")
                    .build();

            CustomerDTO customerDTO = createCustomerDTO(customerId, "test@example.com", "Johnny", "Doe Updated");

            when(customerService.updateCustomer(eq(customerId), any(UpdateCustomerRequest.class)))
                    .thenReturn(customerDTO);

            // When & Then
            mockMvc.perform(put("/api/customers/{customerId}", customerId)
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                    .jwt(jwt -> jwt
                                            .claim("realm_access", Map.of("roles", List.of("ADMIN")))
                                            .subject(UUID.randomUUID().toString())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.firstName").value("Johnny"));

            verify(customerService).updateCustomer(eq(customerId), any(UpdateCustomerRequest.class));
        }
    }

    @Nested
    @DisplayName("Update Tier Tests")
    class UpdateTierTests {

        @Test
        @DisplayName("Should update customer tier as ADMIN")
        void shouldUpdateCustomerTierAsAdmin() throws Exception {
            // Given
            CustomerDTO customerDTO = createCustomerDTO(customerId, "test@example.com", "John", "Doe");
            customerDTO.setTier(CustomerTier.VIP);

            when(customerService.updateCustomerTier(customerId, CustomerTier.VIP))
                    .thenReturn(customerDTO);

            // When & Then
            mockMvc.perform(put("/api/customers/{customerId}/tier", customerId)
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                    .jwt(jwt -> jwt
                                            .claim("realm_access", Map.of("roles", List.of("ADMIN")))
                                            .subject(UUID.randomUUID().toString())))
                            .param("tier", "VIP"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.tier").value("VIP"));

            verify(customerService).updateCustomerTier(customerId, CustomerTier.VIP);
        }
    }

    @Nested
    @DisplayName("Authentication Tests")
    class AuthenticationTests {

        @Test
        @DisplayName("Should reject request without authentication")
        void shouldRejectRequestWithoutAuthentication() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/customers/{customerId}", customerId))
                    .andExpect(status().isUnauthorized());

            verify(customerService, never()).getCustomerById(any());
        }
    }

    private CustomerDTO createCustomerDTO(UUID id, String email, String firstName, String lastName) {
        return CustomerDTO.builder()
                .id(id)
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .tier(CustomerTier.STANDARD)
                .status(CustomerStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
