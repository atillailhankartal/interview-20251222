package com.brokage.asset.controller;

import com.brokage.asset.config.TestSecurityConfig;
import com.brokage.asset.dto.CustomerAssetDTO;
import com.brokage.asset.dto.DepositWithdrawRequest;
import com.brokage.asset.service.AssetService;
import com.brokage.common.dto.PageResponse;
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
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AssetController.class)
@Import(TestSecurityConfig.class)
class AssetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AssetService assetService;

    private UUID customerId;
    private UUID assetId;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        assetId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("Get Customer Assets Tests")
    class GetCustomerAssetsTests {

        @Test
        @DisplayName("Should get customer assets as ADMIN")
        void shouldGetCustomerAssetsAsAdmin() throws Exception {
            // Given
            List<CustomerAssetDTO> assets = List.of(
                    createAssetDTO(assetId, customerId, "TRY", new BigDecimal("10000")),
                    createAssetDTO(UUID.randomUUID(), customerId, "AAPL", new BigDecimal("50"))
            );

            when(assetService.getCustomerAssets(customerId)).thenReturn(assets);

            // When & Then
            mockMvc.perform(get("/api/assets")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                    .jwt(jwt -> jwt
                                            .claim("realm_access", Map.of("roles", List.of("ADMIN")))
                                            .subject(UUID.randomUUID().toString())))
                            .param("customerId", customerId.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray());

            verify(assetService).getCustomerAssets(customerId);
        }

        @Test
        @DisplayName("Should get own assets as CUSTOMER")
        void shouldGetOwnAssetsAsCustomer() throws Exception {
            // Given
            List<CustomerAssetDTO> assets = List.of(
                    createAssetDTO(assetId, customerId, "TRY", new BigDecimal("10000"))
            );

            when(assetService.getCustomerAssets(customerId)).thenReturn(assets);

            // When & Then
            mockMvc.perform(get("/api/assets")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
                                    .jwt(jwt -> jwt
                                            .claim("realm_access", Map.of("roles", List.of("CUSTOMER")))
                                            .subject(customerId.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(assetService).getCustomerAssets(customerId);
        }
    }

    @Nested
    @DisplayName("Get Paged Assets Tests")
    class GetPagedAssetsTests {

        @Test
        @DisplayName("Should get paged assets")
        void shouldGetPagedAssets() throws Exception {
            // Given
            List<CustomerAssetDTO> assets = List.of(
                    createAssetDTO(assetId, customerId, "AAPL", new BigDecimal("100"))
            );
            Page<CustomerAssetDTO> page = new PageImpl<>(assets);
            PageResponse<CustomerAssetDTO> pageResponse = PageResponse.of(page);

            when(assetService.getCustomerAssetsPaged(eq(customerId), eq(0), eq(20)))
                    .thenReturn(pageResponse);

            // When & Then
            mockMvc.perform(get("/api/assets/paged")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                    .jwt(jwt -> jwt
                                            .claim("realm_access", Map.of("roles", List.of("ADMIN")))
                                            .subject(UUID.randomUUID().toString())))
                            .param("customerId", customerId.toString())
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(assetService).getCustomerAssetsPaged(customerId, 0, 20);
        }
    }

    @Nested
    @DisplayName("Get Asset by Symbol Tests")
    class GetAssetBySymbolTests {

        @Test
        @DisplayName("Should get asset by symbol")
        void shouldGetAssetBySymbol() throws Exception {
            // Given
            CustomerAssetDTO assetDTO = createAssetDTO(assetId, customerId, "AAPL", new BigDecimal("100"));

            when(assetService.getCustomerAsset(customerId, "AAPL")).thenReturn(assetDTO);

            // When & Then
            mockMvc.perform(get("/api/assets/{assetName}", "AAPL")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                    .jwt(jwt -> jwt
                                            .claim("realm_access", Map.of("roles", List.of("ADMIN")))
                                            .subject(UUID.randomUUID().toString())))
                            .param("customerId", customerId.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.assetName").value("AAPL"));

            verify(assetService).getCustomerAsset(customerId, "AAPL");
        }
    }

    @Nested
    @DisplayName("Deposit Tests")
    class DepositTests {

        @Test
        @DisplayName("Should deposit as ADMIN")
        void shouldDepositAsAdmin() throws Exception {
            // Given
            DepositWithdrawRequest request = DepositWithdrawRequest.builder()
                    .customerId(customerId)
                    .assetName("TRY")
                    .amount(new BigDecimal("5000"))
                    .build();

            CustomerAssetDTO assetDTO = createAssetDTO(assetId, customerId, "TRY", new BigDecimal("15000"));

            when(assetService.deposit(any(DepositWithdrawRequest.class))).thenReturn(assetDTO);

            // When & Then
            mockMvc.perform(post("/api/assets/deposit")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                    .jwt(jwt -> jwt
                                            .claim("realm_access", Map.of("roles", List.of("ADMIN")))
                                            .subject(UUID.randomUUID().toString())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(assetService).deposit(any(DepositWithdrawRequest.class));
        }

        @Test
        @DisplayName("Should reject deposit for CUSTOMER")
        void shouldRejectDepositForCustomer() throws Exception {
            // Given
            DepositWithdrawRequest request = DepositWithdrawRequest.builder()
                    .customerId(customerId)
                    .assetName("TRY")
                    .amount(new BigDecimal("5000"))
                    .build();

            // When & Then
            mockMvc.perform(post("/api/assets/deposit")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
                                    .jwt(jwt -> jwt
                                            .claim("realm_access", Map.of("roles", List.of("CUSTOMER")))
                                            .subject(customerId.toString())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());

            verify(assetService, never()).deposit(any());
        }
    }

    @Nested
    @DisplayName("Withdraw Tests")
    class WithdrawTests {

        @Test
        @DisplayName("Should withdraw as ADMIN")
        void shouldWithdrawAsAdmin() throws Exception {
            // Given
            DepositWithdrawRequest request = DepositWithdrawRequest.builder()
                    .customerId(customerId)
                    .assetName("TRY")
                    .amount(new BigDecimal("2000"))
                    .build();

            CustomerAssetDTO assetDTO = createAssetDTO(assetId, customerId, "TRY", new BigDecimal("8000"));

            when(assetService.withdraw(any(DepositWithdrawRequest.class))).thenReturn(assetDTO);

            // When & Then
            mockMvc.perform(post("/api/assets/withdraw")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                    .jwt(jwt -> jwt
                                            .claim("realm_access", Map.of("roles", List.of("ADMIN")))
                                            .subject(UUID.randomUUID().toString())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(assetService).withdraw(any(DepositWithdrawRequest.class));
        }
    }

    @Nested
    @DisplayName("Authentication Tests")
    class AuthenticationTests {

        @Test
        @DisplayName("Should reject request without authentication")
        void shouldRejectRequestWithoutAuthentication() throws Exception {
            // When & Then - without JWT, expect 4xx error
            mockMvc.perform(get("/api/assets"))
                    .andExpect(status().is4xxClientError());

            verify(assetService, never()).getCustomerAssets(any());
        }
    }

    private CustomerAssetDTO createAssetDTO(UUID assetId, UUID customerId, String symbol, BigDecimal usableSize) {
        return CustomerAssetDTO.builder()
                .id(assetId)
                .customerId(customerId)
                .assetName(symbol)
                .size(usableSize)
                .usableSize(usableSize)
                .blockedSize(BigDecimal.ZERO)
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
