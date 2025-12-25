package com.brokage.asset.service;

import com.brokage.asset.dto.CustomerAssetDTO;
import com.brokage.asset.dto.DepositWithdrawRequest;
import com.brokage.asset.entity.CustomerAsset;
import com.brokage.asset.entity.OutboxEvent;
import com.brokage.asset.repository.CustomerAssetRepository;
import com.brokage.asset.repository.OutboxEventRepository;
import com.brokage.common.exception.BusinessException;
import com.brokage.common.exception.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssetServiceTest {

    @Mock
    private CustomerAssetRepository customerAssetRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private AssetService assetService;

    private UUID customerId;
    private UUID assetId;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        assetId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("Deposit Tests")
    class DepositTests {

        @Test
        @DisplayName("Should deposit to existing asset")
        void shouldDepositToExistingAsset() throws Exception {
            // Given
            DepositWithdrawRequest request = DepositWithdrawRequest.builder()
                    .customerId(customerId)
                    .assetName("TRY")
                    .amount(BigDecimal.valueOf(1000))
                    .build();

            CustomerAsset existingAsset = createAsset(assetId, customerId, "TRY",
                    BigDecimal.valueOf(5000), BigDecimal.valueOf(5000));

            when(customerAssetRepository.findByCustomerIdAndAssetNameForUpdate(customerId, "TRY"))
                    .thenReturn(Optional.of(existingAsset));
            when(customerAssetRepository.save(any(CustomerAsset.class))).thenReturn(existingAsset);
            when(objectMapper.writeValueAsString(any())).thenReturn("{}");

            // When
            CustomerAssetDTO result = assetService.deposit(request);

            // Then
            assertThat(result.getUsableSize()).isEqualTo(BigDecimal.valueOf(6000));
            verify(outboxEventRepository).save(any(OutboxEvent.class));
        }

        @Test
        @DisplayName("Should create new asset when depositing to non-existing")
        void shouldCreateNewAssetWhenDepositingToNonExisting() throws Exception {
            // Given
            DepositWithdrawRequest request = DepositWithdrawRequest.builder()
                    .customerId(customerId)
                    .assetName("AAPL")
                    .amount(BigDecimal.valueOf(100))
                    .build();

            CustomerAsset newAsset = createAsset(assetId, customerId, "AAPL",
                    BigDecimal.valueOf(100), BigDecimal.valueOf(100));

            when(customerAssetRepository.findByCustomerIdAndAssetNameForUpdate(customerId, "AAPL"))
                    .thenReturn(Optional.empty());
            when(customerAssetRepository.save(any(CustomerAsset.class))).thenReturn(newAsset);
            when(objectMapper.writeValueAsString(any())).thenReturn("{}");

            // When
            CustomerAssetDTO result = assetService.deposit(request);

            // Then
            assertThat(result.getAssetName()).isEqualTo("AAPL");
            assertThat(result.getUsableSize()).isEqualTo(BigDecimal.valueOf(100));
        }

        @Test
        @DisplayName("Should throw exception for negative amount")
        void shouldThrowExceptionForNegativeAmount() {
            // Given
            DepositWithdrawRequest request = DepositWithdrawRequest.builder()
                    .customerId(customerId)
                    .assetName("TRY")
                    .amount(BigDecimal.valueOf(-100))
                    .build();

            // When & Then
            assertThatThrownBy(() -> assetService.deposit(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("positive");
        }
    }

    @Nested
    @DisplayName("Withdraw Tests")
    class WithdrawTests {

        @Test
        @DisplayName("Should withdraw from existing asset with sufficient balance")
        void shouldWithdrawFromExistingAssetWithSufficientBalance() throws Exception {
            // Given
            DepositWithdrawRequest request = DepositWithdrawRequest.builder()
                    .customerId(customerId)
                    .assetName("TRY")
                    .amount(BigDecimal.valueOf(1000))
                    .build();

            CustomerAsset existingAsset = createAsset(assetId, customerId, "TRY",
                    BigDecimal.valueOf(5000), BigDecimal.valueOf(5000));

            when(customerAssetRepository.findByCustomerIdAndAssetNameForUpdate(customerId, "TRY"))
                    .thenReturn(Optional.of(existingAsset));
            when(customerAssetRepository.save(any(CustomerAsset.class))).thenReturn(existingAsset);
            when(objectMapper.writeValueAsString(any())).thenReturn("{}");

            // When
            CustomerAssetDTO result = assetService.withdraw(request);

            // Then
            assertThat(result.getUsableSize()).isEqualTo(BigDecimal.valueOf(4000));
        }

        @Test
        @DisplayName("Should throw exception for insufficient balance")
        void shouldThrowExceptionForInsufficientBalance() {
            // Given
            DepositWithdrawRequest request = DepositWithdrawRequest.builder()
                    .customerId(customerId)
                    .assetName("TRY")
                    .amount(BigDecimal.valueOf(10000))
                    .build();

            CustomerAsset existingAsset = createAsset(assetId, customerId, "TRY",
                    BigDecimal.valueOf(5000), BigDecimal.valueOf(5000));

            when(customerAssetRepository.findByCustomerIdAndAssetNameForUpdate(customerId, "TRY"))
                    .thenReturn(Optional.of(existingAsset));

            // When & Then
            assertThatThrownBy(() -> assetService.withdraw(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Insufficient");
        }

        @Test
        @DisplayName("Should throw exception when asset not found")
        void shouldThrowExceptionWhenAssetNotFound() {
            // Given
            DepositWithdrawRequest request = DepositWithdrawRequest.builder()
                    .customerId(customerId)
                    .assetName("UNKNOWN")
                    .amount(BigDecimal.valueOf(100))
                    .build();

            when(customerAssetRepository.findByCustomerIdAndAssetNameForUpdate(customerId, "UNKNOWN"))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> assetService.withdraw(request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Reserve Asset Tests")
    class ReserveAssetTests {

        @Test
        @DisplayName("Should reserve asset successfully")
        void shouldReserveAssetSuccessfully() throws Exception {
            // Given
            CustomerAsset asset = createAsset(assetId, customerId, "TRY",
                    BigDecimal.valueOf(10000), BigDecimal.valueOf(10000));

            when(customerAssetRepository.findByCustomerIdAndAssetNameForUpdate(customerId, "TRY"))
                    .thenReturn(Optional.of(asset));
            when(customerAssetRepository.save(any(CustomerAsset.class))).thenReturn(asset);
            when(objectMapper.writeValueAsString(any())).thenReturn("{}");

            // When
            CustomerAssetDTO result = assetService.reserveAsset(customerId, "TRY", BigDecimal.valueOf(5000));

            // Then
            assertThat(result.getUsableSize()).isEqualTo(BigDecimal.valueOf(5000));
            assertThat(result.getBlockedSize()).isEqualTo(BigDecimal.valueOf(5000));
        }

        @Test
        @DisplayName("Should throw exception when insufficient balance for reservation")
        void shouldThrowExceptionWhenInsufficientBalanceForReservation() {
            // Given
            CustomerAsset asset = createAsset(assetId, customerId, "TRY",
                    BigDecimal.valueOf(1000), BigDecimal.valueOf(1000));

            when(customerAssetRepository.findByCustomerIdAndAssetNameForUpdate(customerId, "TRY"))
                    .thenReturn(Optional.of(asset));

            // When & Then
            assertThatThrownBy(() -> assetService.reserveAsset(customerId, "TRY", BigDecimal.valueOf(5000)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Insufficient");
        }
    }

    @Nested
    @DisplayName("Release Reservation Tests")
    class ReleaseReservationTests {

        @Test
        @DisplayName("Should release reservation successfully")
        void shouldReleaseReservationSuccessfully() throws Exception {
            // Given - size=10000, usableSize=5000 means blocked=5000
            CustomerAsset asset = createAsset(assetId, customerId, "TRY",
                    BigDecimal.valueOf(10000), BigDecimal.valueOf(5000));

            when(customerAssetRepository.findByCustomerIdAndAssetNameForUpdate(customerId, "TRY"))
                    .thenReturn(Optional.of(asset));
            when(customerAssetRepository.save(any(CustomerAsset.class))).thenReturn(asset);
            when(objectMapper.writeValueAsString(any())).thenReturn("{}");

            // When
            CustomerAssetDTO result = assetService.releaseReservation(customerId, "TRY", BigDecimal.valueOf(5000));

            // Then
            assertThat(result.getUsableSize()).isEqualTo(BigDecimal.valueOf(10000));
            assertThat(result.getBlockedSize()).isEqualTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("Settle Transaction Tests")
    class SettleTransactionTests {

        @Test
        @DisplayName("Should settle BUY transaction (TRY -> AAPL)")
        void shouldSettleBuyTransaction() throws Exception {
            // Given - TRY: size=10000, usable=5000, blocked=5000 (reserved for buy)
            CustomerAsset tryAsset = createAsset(assetId, customerId, "TRY",
                    BigDecimal.valueOf(10000), BigDecimal.valueOf(5000));
            // AAPL: size=10, usable=10, blocked=0
            CustomerAsset aaplAsset = createAsset(UUID.randomUUID(), customerId, "AAPL",
                    BigDecimal.valueOf(10), BigDecimal.valueOf(10));

            when(customerAssetRepository.findByCustomerIdAndAssetNameForUpdate(customerId, "TRY"))
                    .thenReturn(Optional.of(tryAsset));
            when(customerAssetRepository.findByCustomerIdAndAssetNameForUpdate(customerId, "AAPL"))
                    .thenReturn(Optional.of(aaplAsset));
            when(customerAssetRepository.save(any(CustomerAsset.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(objectMapper.writeValueAsString(any())).thenReturn("{}");

            // When - settle 5000 TRY for 50 AAPL
            assetService.settleTransaction(customerId, "TRY", BigDecimal.valueOf(5000),
                    "AAPL", BigDecimal.valueOf(50));

            // Then
            verify(customerAssetRepository, times(2)).save(any(CustomerAsset.class));
            // TRY: size reduced by 5000 (from blocked), now size=5000, usable=5000, blocked=0
            assertThat(tryAsset.getBlockedSize()).isEqualTo(BigDecimal.ZERO);
            // AAPL: size and usable increased by 50
            assertThat(aaplAsset.getUsableSize()).isEqualTo(BigDecimal.valueOf(60));
        }
    }

    @Nested
    @DisplayName("Get Customer Assets Tests")
    class GetCustomerAssetsTests {

        @Test
        @DisplayName("Should get all customer assets")
        void shouldGetAllCustomerAssets() {
            // Given
            List<CustomerAsset> assets = List.of(
                    createAsset(UUID.randomUUID(), customerId, "TRY", BigDecimal.valueOf(10000), BigDecimal.valueOf(10000)),
                    createAsset(UUID.randomUUID(), customerId, "AAPL", BigDecimal.valueOf(50), BigDecimal.valueOf(50))
            );

            when(customerAssetRepository.findByCustomerId(customerId)).thenReturn(assets);

            // When
            List<CustomerAssetDTO> result = assetService.getCustomerAssets(customerId);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getAssetName()).isEqualTo("TRY");
            assertThat(result.get(1).getAssetName()).isEqualTo("AAPL");
        }

        @Test
        @DisplayName("Should get specific customer asset")
        void shouldGetSpecificCustomerAsset() {
            // Given
            CustomerAsset asset = createAsset(assetId, customerId, "TRY",
                    BigDecimal.valueOf(10000), BigDecimal.valueOf(10000));

            when(customerAssetRepository.findByCustomerIdAndAssetName(customerId, "TRY"))
                    .thenReturn(Optional.of(asset));

            // When
            CustomerAssetDTO result = assetService.getCustomerAsset(customerId, "TRY");

            // Then
            assertThat(result.getAssetName()).isEqualTo("TRY");
            assertThat(result.getUsableSize()).isEqualTo(BigDecimal.valueOf(10000));
        }
    }

    // Helper: size = total, usableSize = available, blocked = size - usableSize
    private CustomerAsset createAsset(UUID id, UUID customerId, String assetName,
                                       BigDecimal size, BigDecimal usableSize) {
        CustomerAsset asset = CustomerAsset.builder()
                .customerId(customerId)
                .assetName(assetName)
                .size(size)
                .usableSize(usableSize)
                .build();

        try {
            var idField = asset.getClass().getSuperclass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(asset, id);
        } catch (Exception e) {
            // Fallback
        }

        return asset;
    }
}
