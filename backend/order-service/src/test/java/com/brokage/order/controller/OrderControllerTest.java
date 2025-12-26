package com.brokage.order.controller;

import com.brokage.common.enums.OrderSide;
import com.brokage.common.enums.OrderStatus;
import com.brokage.common.enums.OrderType;
import com.brokage.order.config.TestSecurityConfig;
import com.brokage.order.client.CustomerClient;
import com.brokage.order.dto.CreateOrderRequest;
import com.brokage.order.dto.OrderDTO;
import com.brokage.order.service.OrderService;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
@Import(TestSecurityConfig.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    @MockBean
    private CustomerClient customerClient;

    private UUID customerId;
    private UUID orderId;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        orderId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("Create Order Tests")
    class CreateOrderTests {

        @Test
        @DisplayName("Should create order successfully as ADMIN")
        void shouldCreateOrderSuccessfullyAsAdmin() throws Exception {
            // Given
            CreateOrderRequest request = CreateOrderRequest.builder()
                    .customerId(customerId)
                    .assetName("AAPL")
                    .orderSide(OrderSide.BUY)
                    .orderType(OrderType.LIMIT)
                    .size(new BigDecimal("10"))
                    .price(new BigDecimal("150.00"))
                    .build();

            OrderDTO orderDTO = createOrderDTO(orderId, customerId, "AAPL", OrderSide.BUY, OrderStatus.PENDING);

            when(orderService.createOrder(any(CreateOrderRequest.class))).thenReturn(orderDTO);

            // When & Then
            mockMvc.perform(post("/api/orders")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                    .jwt(jwt -> jwt
                                            .claim("realm_access", java.util.Map.of("roles", List.of("ADMIN")))
                                            .subject(customerId.toString())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").exists());

            verify(orderService).createOrder(any(CreateOrderRequest.class));
        }

        @Test
        @DisplayName("Should reject order creation without authentication")
        void shouldRejectOrderCreationWithoutAuthentication() throws Exception {
            // Given
            CreateOrderRequest request = CreateOrderRequest.builder()
                    .customerId(customerId)
                    .assetName("AAPL")
                    .orderSide(OrderSide.BUY)
                    .orderType(OrderType.LIMIT)
                    .size(new BigDecimal("10"))
                    .price(new BigDecimal("150.00"))
                    .build();

            // When & Then - without JWT, expect either 401 or 403
            mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().is4xxClientError());

            verify(orderService, never()).createOrder(any());
        }
    }

    @Nested
    @DisplayName("Get Order Tests")
    class GetOrderTests {

        @Test
        @DisplayName("Should get order by ID as ADMIN")
        void shouldGetOrderByIdAsAdmin() throws Exception {
            // Given
            OrderDTO orderDTO = createOrderDTO(orderId, customerId, "AAPL", OrderSide.BUY, OrderStatus.PENDING);

            when(orderService.getOrderById(orderId)).thenReturn(orderDTO);

            // When & Then
            mockMvc.perform(get("/api/orders/{orderId}", orderId)
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                    .jwt(jwt -> jwt
                                            .claim("realm_access", java.util.Map.of("roles", List.of("ADMIN")))
                                            .subject(customerId.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(orderId.toString()));

            verify(orderService).getOrderById(orderId);
        }

        @Test
        @DisplayName("Should get order by ID as CUSTOMER for own order")
        void shouldGetOrderByIdAsCustomerForOwnOrder() throws Exception {
            // Given
            OrderDTO orderDTO = createOrderDTO(orderId, customerId, "AAPL", OrderSide.BUY, OrderStatus.PENDING);

            // New flow: getOrderById is called first to check ownership
            when(orderService.getOrderById(orderId)).thenReturn(orderDTO);

            // When & Then
            mockMvc.perform(get("/api/orders/{orderId}", orderId)
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
                                    .jwt(jwt -> jwt
                                            .claim("realm_access", java.util.Map.of("roles", List.of("CUSTOMER")))
                                            .claim("customer_id", customerId.toString())
                                            .subject(customerId.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(orderService).getOrderById(orderId);
        }
    }

    @Nested
    @DisplayName("List Orders Tests")
    class ListOrdersTests {

        @Test
        @DisplayName("Should list all orders as ADMIN")
        void shouldListAllOrdersAsAdmin() throws Exception {
            // Given
            List<OrderDTO> orders = List.of(
                    createOrderDTO(UUID.randomUUID(), customerId, "AAPL", OrderSide.BUY, OrderStatus.PENDING),
                    createOrderDTO(UUID.randomUUID(), customerId, "GOOG", OrderSide.SELL, OrderStatus.MATCHED)
            );
            Page<OrderDTO> orderPage = new PageImpl<>(orders);

            when(orderService.listOrders(any())).thenReturn(orderPage);

            // When & Then
            mockMvc.perform(get("/api/orders")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                    .jwt(jwt -> jwt
                                            .claim("realm_access", java.util.Map.of("roles", List.of("ADMIN")))
                                            .subject(customerId.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isArray());

            verify(orderService).listOrders(any());
        }

        @Test
        @DisplayName("Should list only own orders as CUSTOMER")
        void shouldListOnlyOwnOrdersAsCustomer() throws Exception {
            // Given
            List<OrderDTO> orders = List.of(
                    createOrderDTO(UUID.randomUUID(), customerId, "AAPL", OrderSide.BUY, OrderStatus.PENDING)
            );
            Page<OrderDTO> orderPage = new PageImpl<>(orders);

            when(orderService.listOrdersForCustomer(eq(customerId), any())).thenReturn(orderPage);

            // When & Then
            mockMvc.perform(get("/api/orders")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
                                    .jwt(jwt -> jwt
                                            .claim("realm_access", java.util.Map.of("roles", List.of("CUSTOMER")))
                                            .claim("customer_id", customerId.toString())
                                            .subject(customerId.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(orderService).listOrdersForCustomer(eq(customerId), any());
        }
    }

    @Nested
    @DisplayName("Cancel Order Tests")
    class CancelOrderTests {

        @Test
        @DisplayName("Should cancel order as ADMIN")
        void shouldCancelOrderAsAdmin() throws Exception {
            // Given
            OrderDTO orderDTO = createOrderDTO(orderId, customerId, "AAPL", OrderSide.BUY, OrderStatus.CANCELED);

            when(orderService.cancelOrder(orderId)).thenReturn(orderDTO);

            // When & Then
            mockMvc.perform(delete("/api/orders/{orderId}", orderId)
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                    .jwt(jwt -> jwt
                                            .claim("realm_access", java.util.Map.of("roles", List.of("ADMIN")))
                                            .subject(customerId.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(orderService).cancelOrder(orderId);
        }

        @Test
        @DisplayName("Should cancel own order as CUSTOMER")
        void shouldCancelOwnOrderAsCustomer() throws Exception {
            // Given
            OrderDTO orderDTO = createOrderDTO(orderId, customerId, "AAPL", OrderSide.BUY, OrderStatus.CANCELED);

            when(orderService.cancelOrderForCustomer(orderId, customerId)).thenReturn(orderDTO);

            // When & Then
            mockMvc.perform(delete("/api/orders/{orderId}", orderId)
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
                                    .jwt(jwt -> jwt
                                            .claim("realm_access", java.util.Map.of("roles", List.of("CUSTOMER")))
                                            .claim("customer_id", customerId.toString())
                                            .subject(customerId.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(orderService).cancelOrderForCustomer(orderId, customerId);
        }
    }

    @Nested
    @DisplayName("Match Order Tests")
    class MatchOrderTests {

        @Test
        @DisplayName("Should match order as ADMIN")
        void shouldMatchOrderAsAdmin() throws Exception {
            // Given
            OrderDTO orderDTO = createOrderDTO(orderId, customerId, "AAPL", OrderSide.BUY, OrderStatus.MATCHED);

            when(orderService.matchOrder(orderId)).thenReturn(orderDTO);

            // When & Then
            mockMvc.perform(post("/api/orders/{orderId}/match", orderId)
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                    .jwt(jwt -> jwt
                                            .claim("realm_access", java.util.Map.of("roles", List.of("ADMIN")))
                                            .subject(customerId.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(orderService).matchOrder(orderId);
        }

        @Test
        @DisplayName("Should deny match order for CUSTOMER")
        void shouldDenyMatchOrderForCustomer() throws Exception {
            // When & Then - CUSTOMER role should not have access to match endpoint
            mockMvc.perform(post("/api/orders/{orderId}/match", orderId)
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
                                    .jwt(jwt -> jwt
                                            .claim("realm_access", java.util.Map.of("roles", List.of("CUSTOMER")))
                                            .claim("customer_id", customerId.toString())
                                            .subject(customerId.toString()))))
                    .andExpect(status().isForbidden());

            verify(orderService, never()).matchOrder(any());
        }
    }

    // ==================== BROKER AUTHORIZATION TESTS ====================

    @Nested
    @DisplayName("Broker Authorization Tests")
    class BrokerAuthorizationTests {

        private UUID brokerId;
        private UUID assignedCustomerId;
        private UUID unassignedCustomerId;

        @BeforeEach
        void setUp() {
            brokerId = UUID.randomUUID();
            assignedCustomerId = UUID.randomUUID();
            unassignedCustomerId = UUID.randomUUID();
        }

        @Test
        @DisplayName("Broker should create order for assigned customer")
        void brokerShouldCreateOrderForAssignedCustomer() throws Exception {
            // Given
            CreateOrderRequest request = CreateOrderRequest.builder()
                    .customerId(assignedCustomerId)
                    .assetName("AAPL")
                    .orderSide(OrderSide.BUY)
                    .orderType(OrderType.LIMIT)
                    .size(new BigDecimal("10"))
                    .price(new BigDecimal("150.00"))
                    .build();

            OrderDTO createdOrder = createOrderDTO(orderId, assignedCustomerId, "AAPL", OrderSide.BUY, OrderStatus.PENDING);

            when(customerClient.isBrokerOfCustomer(brokerId, assignedCustomerId)).thenReturn(true);
            when(orderService.createOrder(any())).thenReturn(createdOrder);

            // When & Then
            mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("ROLE_BROKER"))
                                    .jwt(jwt -> jwt
                                            .claim("realm_access", java.util.Map.of("roles", List.of("BROKER")))
                                            .claim("customer_id", brokerId.toString())
                                            .subject(brokerId.toString()))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true));

            verify(customerClient).isBrokerOfCustomer(brokerId, assignedCustomerId);
            verify(orderService).createOrder(any());
        }

        @Test
        @DisplayName("Broker should NOT create order for non-assigned customer")
        void brokerShouldNotCreateOrderForNonAssignedCustomer() throws Exception {
            // Given
            CreateOrderRequest request = CreateOrderRequest.builder()
                    .customerId(unassignedCustomerId)
                    .assetName("AAPL")
                    .orderSide(OrderSide.BUY)
                    .orderType(OrderType.LIMIT)
                    .size(new BigDecimal("10"))
                    .price(new BigDecimal("150.00"))
                    .build();

            when(customerClient.isBrokerOfCustomer(brokerId, unassignedCustomerId)).thenReturn(false);

            // When & Then
            mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("ROLE_BROKER"))
                                    .jwt(jwt -> jwt
                                            .claim("realm_access", java.util.Map.of("roles", List.of("BROKER")))
                                            .claim("customer_id", brokerId.toString())
                                            .subject(brokerId.toString()))))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false));

            verify(customerClient).isBrokerOfCustomer(brokerId, unassignedCustomerId);
            verify(orderService, never()).createOrder(any());
        }

        @Test
        @DisplayName("Broker should view assigned customer's order")
        void brokerShouldViewAssignedCustomerOrder() throws Exception {
            // Given
            OrderDTO orderDTO = createOrderDTO(orderId, assignedCustomerId, "AAPL", OrderSide.BUY, OrderStatus.PENDING);

            when(orderService.getOrderById(orderId)).thenReturn(orderDTO);
            when(customerClient.isBrokerOfCustomer(brokerId, assignedCustomerId)).thenReturn(true);

            // When & Then
            mockMvc.perform(get("/api/orders/{orderId}", orderId)
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("ROLE_BROKER"))
                                    .jwt(jwt -> jwt
                                            .claim("realm_access", java.util.Map.of("roles", List.of("BROKER")))
                                            .claim("customer_id", brokerId.toString())
                                            .subject(brokerId.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(customerClient).isBrokerOfCustomer(brokerId, assignedCustomerId);
        }

        @Test
        @DisplayName("Broker should NOT view non-assigned customer's order")
        void brokerShouldNotViewNonAssignedCustomerOrder() throws Exception {
            // Given
            OrderDTO orderDTO = createOrderDTO(orderId, unassignedCustomerId, "AAPL", OrderSide.BUY, OrderStatus.PENDING);

            when(orderService.getOrderById(orderId)).thenReturn(orderDTO);
            when(customerClient.isBrokerOfCustomer(brokerId, unassignedCustomerId)).thenReturn(false);

            // When & Then
            mockMvc.perform(get("/api/orders/{orderId}", orderId)
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("ROLE_BROKER"))
                                    .jwt(jwt -> jwt
                                            .claim("realm_access", java.util.Map.of("roles", List.of("BROKER")))
                                            .claim("customer_id", brokerId.toString())
                                            .subject(brokerId.toString()))))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false));

            verify(customerClient).isBrokerOfCustomer(brokerId, unassignedCustomerId);
        }

        @Test
        @DisplayName("Broker should cancel assigned customer's order")
        void brokerShouldCancelAssignedCustomerOrder() throws Exception {
            // Given
            OrderDTO orderDTO = createOrderDTO(orderId, assignedCustomerId, "AAPL", OrderSide.BUY, OrderStatus.PENDING);
            OrderDTO cancelledOrder = createOrderDTO(orderId, assignedCustomerId, "AAPL", OrderSide.BUY, OrderStatus.CANCELED);

            when(orderService.getOrderById(orderId)).thenReturn(orderDTO);
            when(customerClient.isBrokerOfCustomer(brokerId, assignedCustomerId)).thenReturn(true);
            when(orderService.cancelOrder(orderId)).thenReturn(cancelledOrder);

            // When & Then
            mockMvc.perform(delete("/api/orders/{orderId}", orderId)
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("ROLE_BROKER"))
                                    .jwt(jwt -> jwt
                                            .claim("realm_access", java.util.Map.of("roles", List.of("BROKER")))
                                            .claim("customer_id", brokerId.toString())
                                            .subject(brokerId.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(customerClient).isBrokerOfCustomer(brokerId, assignedCustomerId);
            verify(orderService).cancelOrder(orderId);
        }

        @Test
        @DisplayName("Broker should match assigned customer's order")
        void brokerShouldMatchAssignedCustomerOrder() throws Exception {
            // Given
            OrderDTO orderDTO = createOrderDTO(orderId, assignedCustomerId, "AAPL", OrderSide.BUY, OrderStatus.PENDING);
            OrderDTO matchedOrder = createOrderDTO(orderId, assignedCustomerId, "AAPL", OrderSide.BUY, OrderStatus.MATCHED);

            when(orderService.getOrderById(orderId)).thenReturn(orderDTO);
            when(customerClient.isBrokerOfCustomer(brokerId, assignedCustomerId)).thenReturn(true);
            when(orderService.matchOrder(orderId)).thenReturn(matchedOrder);

            // When & Then
            mockMvc.perform(post("/api/orders/{orderId}/match", orderId)
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("ROLE_BROKER"))
                                    .jwt(jwt -> jwt
                                            .claim("realm_access", java.util.Map.of("roles", List.of("BROKER")))
                                            .claim("customer_id", brokerId.toString())
                                            .subject(brokerId.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(customerClient).isBrokerOfCustomer(brokerId, assignedCustomerId);
            verify(orderService).matchOrder(orderId);
        }

        @Test
        @DisplayName("Broker should NOT match non-assigned customer's order")
        void brokerShouldNotMatchNonAssignedCustomerOrder() throws Exception {
            // Given
            OrderDTO orderDTO = createOrderDTO(orderId, unassignedCustomerId, "AAPL", OrderSide.BUY, OrderStatus.PENDING);

            when(orderService.getOrderById(orderId)).thenReturn(orderDTO);
            when(customerClient.isBrokerOfCustomer(brokerId, unassignedCustomerId)).thenReturn(false);

            // When & Then
            mockMvc.perform(post("/api/orders/{orderId}/match", orderId)
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("ROLE_BROKER"))
                                    .jwt(jwt -> jwt
                                            .claim("realm_access", java.util.Map.of("roles", List.of("BROKER")))
                                            .claim("customer_id", brokerId.toString())
                                            .subject(brokerId.toString()))))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false));

            verify(customerClient).isBrokerOfCustomer(brokerId, unassignedCustomerId);
            verify(orderService, never()).matchOrder(any());
        }
    }

    private OrderDTO createOrderDTO(UUID orderId, UUID customerId, String assetName,
                                     OrderSide orderSide, OrderStatus status) {
        return OrderDTO.builder()
                .id(orderId)
                .customerId(customerId)
                .assetName(assetName)
                .orderSide(orderSide)
                .orderType(OrderType.LIMIT)
                .size(new BigDecimal("10"))
                .price(new BigDecimal("150.00"))
                .status(status)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
