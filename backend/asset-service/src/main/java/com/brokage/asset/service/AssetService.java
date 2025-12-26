package com.brokage.asset.service;

import com.brokage.asset.dto.AssetStatsDTO;
import com.brokage.asset.dto.CustomerAssetDTO;
import com.brokage.asset.dto.DepositWithdrawRequest;
import com.brokage.asset.entity.CustomerAsset;
import com.brokage.asset.entity.OutboxEvent;
import com.brokage.asset.repository.CustomerAssetRepository;
import com.brokage.asset.repository.OutboxEventRepository;
import com.brokage.common.dto.PageResponse;
import com.brokage.common.exception.BusinessException;
import com.brokage.common.exception.ResourceNotFoundException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssetService {

    private final CustomerAssetRepository customerAssetRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    private static final String TRY_ASSET = "TRY";

    @Transactional(readOnly = true)
    public List<CustomerAssetDTO> getCustomerAssets(UUID customerId) {
        log.debug("Getting assets for customer: {}", customerId);
        return customerAssetRepository.findByCustomerId(customerId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PageResponse<CustomerAssetDTO> getCustomerAssetsPaged(UUID customerId, int page, int size) {
        log.debug("Getting paged assets for customer: {}, page: {}, size: {}", customerId, page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.by("assetName").ascending());
        Page<CustomerAsset> assetPage = customerAssetRepository.findByCustomerId(customerId, pageable);
        Page<CustomerAssetDTO> dtoPage = assetPage.map(this::toDTO);
        return PageResponse.of(dtoPage);
    }

    @Transactional(readOnly = true)
    public CustomerAssetDTO getCustomerAsset(UUID customerId, String assetName) {
        log.debug("Getting asset {} for customer: {}", assetName, customerId);
        CustomerAsset asset = customerAssetRepository
                .findByCustomerIdAndAssetName(customerId, assetName)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Asset not found for customer: " + customerId + ", name: " + assetName));
        return toDTO(asset);
    }

    @Transactional
    public CustomerAssetDTO deposit(DepositWithdrawRequest request) {
        log.info("Processing deposit: customerId={}, asset={}, amount={}",
                request.getCustomerId(), request.getAssetName(), request.getAmount());

        validatePositiveAmount(request.getAmount());

        CustomerAsset asset = customerAssetRepository
                .findByCustomerIdAndAssetNameForUpdate(request.getCustomerId(), request.getAssetName())
                .orElseGet(() -> createNewAsset(request.getCustomerId(), request.getAssetName()));

        asset.addUsableAmount(request.getAmount());
        CustomerAsset savedAsset = customerAssetRepository.save(asset);

        createOutboxEvent("DepositCompletedEvent", savedAsset.getId(), Map.of(
                "customerId", request.getCustomerId().toString(),
                "assetName", request.getAssetName(),
                "amount", request.getAmount().toString(),
                "newBalance", savedAsset.getUsableSize().toString()
        ));

        log.info("Deposit completed: customerId={}, asset={}, newBalance={}",
                request.getCustomerId(), request.getAssetName(), savedAsset.getUsableSize());

        return toDTO(savedAsset);
    }

    @Transactional
    public CustomerAssetDTO withdraw(DepositWithdrawRequest request) {
        log.info("Processing withdrawal: customerId={}, asset={}, amount={}",
                request.getCustomerId(), request.getAssetName(), request.getAmount());

        validatePositiveAmount(request.getAmount());

        CustomerAsset asset = customerAssetRepository
                .findByCustomerIdAndAssetNameForUpdate(request.getCustomerId(), request.getAssetName())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Asset not found for customer: " + request.getCustomerId()));

        if (!asset.hasAvailableBalance(request.getAmount())) {
            throw new BusinessException("Insufficient usable balance. Available: " + asset.getUsableSize());
        }

        // Withdraw reduces both size and usableSize
        asset.setSize(asset.getSize().subtract(request.getAmount()));
        asset.setUsableSize(asset.getUsableSize().subtract(request.getAmount()));
        CustomerAsset savedAsset = customerAssetRepository.save(asset);

        createOutboxEvent("WithdrawalCompletedEvent", savedAsset.getId(), Map.of(
                "customerId", request.getCustomerId().toString(),
                "assetName", request.getAssetName(),
                "amount", request.getAmount().toString(),
                "newBalance", savedAsset.getUsableSize().toString()
        ));

        log.info("Withdrawal completed: customerId={}, asset={}, newBalance={}",
                request.getCustomerId(), request.getAssetName(), savedAsset.getUsableSize());

        return toDTO(savedAsset);
    }

    @Transactional
    public CustomerAssetDTO reserveAsset(UUID customerId, String assetName, BigDecimal amount) {
        log.info("Reserving asset: customerId={}, asset={}, amount={}", customerId, assetName, amount);

        validatePositiveAmount(amount);

        CustomerAsset asset = customerAssetRepository
                .findByCustomerIdAndAssetNameForUpdate(customerId, assetName)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Asset not found for customer: " + customerId + ", name: " + assetName));

        if (!asset.hasAvailableBalance(amount)) {
            throw new BusinessException("Insufficient usable balance for reservation. Available: " + asset.getUsableSize());
        }

        asset.blockAmount(amount);
        CustomerAsset savedAsset = customerAssetRepository.save(asset);

        createOutboxEvent("AssetReservedEvent", savedAsset.getId(), Map.of(
                "customerId", customerId.toString(),
                "assetName", assetName,
                "reservedAmount", amount.toString(),
                "usableBalance", savedAsset.getUsableSize().toString(),
                "blockedBalance", savedAsset.getBlockedSize().toString()
        ));

        log.info("Asset reserved: customerId={}, asset={}, blocked={}", customerId, assetName, amount);

        return toDTO(savedAsset);
    }

    @Transactional
    public CustomerAssetDTO releaseReservation(UUID customerId, String assetName, BigDecimal amount) {
        log.info("Releasing reservation: customerId={}, asset={}, amount={}", customerId, assetName, amount);

        validatePositiveAmount(amount);

        CustomerAsset asset = customerAssetRepository
                .findByCustomerIdAndAssetNameForUpdate(customerId, assetName)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Asset not found for customer: " + customerId + ", name: " + assetName));

        asset.releaseBlockedAmount(amount);
        CustomerAsset savedAsset = customerAssetRepository.save(asset);

        createOutboxEvent("AssetReleasedEvent", savedAsset.getId(), Map.of(
                "customerId", customerId.toString(),
                "assetName", assetName,
                "releasedAmount", amount.toString(),
                "usableBalance", savedAsset.getUsableSize().toString(),
                "blockedBalance", savedAsset.getBlockedSize().toString()
        ));

        log.info("Reservation released: customerId={}, asset={}, released={}", customerId, assetName, amount);

        return toDTO(savedAsset);
    }

    @Transactional
    public void settleTransaction(UUID customerId, String fromAsset, BigDecimal fromAmount,
                                   String toAsset, BigDecimal toAmount) {
        log.info("Settling transaction: customerId={}, {} {} -> {} {}",
                customerId, fromAmount, fromAsset, toAmount, toAsset);

        // Deduct from blocked amount (reduces size)
        CustomerAsset sourceAsset = customerAssetRepository
                .findByCustomerIdAndAssetNameForUpdate(customerId, fromAsset)
                .orElseThrow(() -> new ResourceNotFoundException("Source asset not found"));

        sourceAsset.deductBlockedAmount(fromAmount);
        customerAssetRepository.save(sourceAsset);

        // Add to destination asset (increases both size and usableSize)
        CustomerAsset destAsset = customerAssetRepository
                .findByCustomerIdAndAssetNameForUpdate(customerId, toAsset)
                .orElseGet(() -> createNewAsset(customerId, toAsset));

        destAsset.addUsableAmount(toAmount);
        customerAssetRepository.save(destAsset);

        createOutboxEvent("SettlementCompletedEvent", sourceAsset.getId(), Map.of(
                "customerId", customerId.toString(),
                "fromAsset", fromAsset,
                "fromAmount", fromAmount.toString(),
                "toAsset", toAsset,
                "toAmount", toAmount.toString()
        ));

        log.info("Transaction settled: customerId={}, deducted {} {}, added {} {}",
                customerId, fromAmount, fromAsset, toAmount, toAsset);
    }

    @Transactional(readOnly = true)
    public BigDecimal getUsableBalance(UUID customerId, String assetName) {
        return customerAssetRepository.getUsableBalance(customerId, assetName);
    }

    /**
     * Get all TRY balances for admin dashboard - returns map of customerId to balance info
     */
    @Transactional(readOnly = true)
    public List<CustomerAssetDTO> getAllTryBalances() {
        log.debug("Getting all TRY balances for admin dashboard");
        return customerAssetRepository.findAllTryAssets()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get asset statistics for analytics dashboard (ADMIN only)
     */
    @Transactional(readOnly = true)
    public AssetStatsDTO getStats() {
        log.debug("Getting asset statistics for analytics");

        List<CustomerAsset> allAssets = customerAssetRepository.findAll();

        BigDecimal totalAUM = allAssets.stream()
                .map(CustomerAsset::getSize)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalTryBalance = allAssets.stream()
                .filter(a -> TRY_ASSET.equals(a.getAssetName()))
                .map(CustomerAsset::getSize)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalCustomersWithAssets = allAssets.stream()
                .map(CustomerAsset::getCustomerId)
                .distinct()
                .count();

        long uniqueAssetTypes = allAssets.stream()
                .map(CustomerAsset::getAssetName)
                .distinct()
                .count();

        BigDecimal largestPosition = allAssets.stream()
                .map(CustomerAsset::getSize)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        return AssetStatsDTO.builder()
                .totalAUM(totalAUM)
                .totalTryBalance(totalTryBalance)
                .totalCustomersWithAssets(totalCustomersWithAssets)
                .uniqueAssetTypes(uniqueAssetTypes)
                .largestPosition(largestPosition)
                .build();
    }

    private CustomerAsset createNewAsset(UUID customerId, String assetName) {
        return CustomerAsset.builder()
                .customerId(customerId)
                .assetName(assetName)
                .size(BigDecimal.ZERO)
                .usableSize(BigDecimal.ZERO)
                .build();
    }

    private void validatePositiveAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Amount must be positive");
        }
    }

    private void createOutboxEvent(String eventType, UUID aggregateId, Map<String, String> payload) {
        try {
            OutboxEvent event = OutboxEvent.builder()
                    .aggregateType("CustomerAsset")
                    .aggregateId(aggregateId)
                    .eventType(eventType)
                    .payload(objectMapper.writeValueAsString(payload))
                    .topic("asset." + eventType.toLowerCase().replace("event", ""))
                    .processed(false)
                    .retryCount(0)
                    .build();
            outboxEventRepository.save(event);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize outbox event payload", e);
            throw new RuntimeException("Failed to create outbox event", e);
        }
    }

    private CustomerAssetDTO toDTO(CustomerAsset asset) {
        return CustomerAssetDTO.builder()
                .id(asset.getId())
                .customerId(asset.getCustomerId())
                .assetName(asset.getAssetName())
                .size(asset.getSize())
                .usableSize(asset.getUsableSize())
                .blockedSize(asset.getBlockedSize())
                .createdAt(asset.getCreatedAt())
                .updatedAt(asset.getUpdatedAt())
                .build();
    }
}
