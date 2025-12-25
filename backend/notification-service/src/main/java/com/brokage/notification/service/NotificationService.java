package com.brokage.notification.service;

import com.brokage.common.dto.PageResponse;
import com.brokage.common.exception.ResourceNotFoundException;
import com.brokage.notification.document.Notification;
import com.brokage.notification.dto.NotificationDTO;
import com.brokage.notification.dto.SendNotificationRequest;
import com.brokage.notification.repository.NotificationRepository;
import com.brokage.notification.sender.NotificationDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationDispatcher notificationDispatcher;

    private static final int DEFAULT_MAX_RETRIES = 3;

    public NotificationDTO createNotification(SendNotificationRequest request, String notificationType, UUID eventId) {
        log.info("Creating notification for customer {} with type {}", request.getCustomerId(), notificationType);

        // Check for idempotency
        if (eventId != null && notificationRepository.findByEventId(eventId).isPresent()) {
            log.info("Notification for event {} already exists, skipping", eventId);
            return toDTO(notificationRepository.findByEventId(eventId).get());
        }

        Notification notification = Notification.builder()
                .eventId(eventId)
                .customerId(request.getCustomerId())
                .notificationType(notificationType)
                .channel(request.getChannel())
                .templateCode(request.getTemplateCode())
                .templateVariables(request.getTemplateVariables())
                .recipient(request.getOverrideRecipient())
                .status(Notification.NotificationStatus.PENDING.name())
                .retryCount(0)
                .maxRetries(DEFAULT_MAX_RETRIES)
                .createdAt(LocalDateTime.now())
                .build();

        Notification savedNotification = notificationRepository.save(notification);
        log.info("Notification created with ID: {}", savedNotification.getId());

        // Dispatch notification asynchronously
        notificationDispatcher.dispatch(savedNotification);

        return toDTO(savedNotification);
    }

    public NotificationDTO createOrderNotification(UUID customerId, UUID orderId, String orderStatus,
                                                    String assetName, String orderSide, String amount) {
        log.info("Creating order notification for customer {} order {}", customerId, orderId);

        String notificationType = "ORDER_" + orderStatus;
        String subject = String.format("Order %s: %s %s %s", orderStatus, orderSide, amount, assetName);
        String body = String.format("Your %s order for %s %s has been %s.",
                orderSide.toLowerCase(), amount, assetName, orderStatus.toLowerCase());

        Notification notification = Notification.builder()
                .eventId(orderId)
                .customerId(customerId)
                .notificationType(notificationType)
                .channel(Notification.NotificationChannel.EMAIL.name())
                .subject(subject)
                .body(body)
                .templateVariables(Map.of(
                        "orderId", orderId.toString(),
                        "orderStatus", orderStatus,
                        "assetName", assetName,
                        "orderSide", orderSide,
                        "amount", amount
                ))
                .status(Notification.NotificationStatus.PENDING.name())
                .retryCount(0)
                .maxRetries(DEFAULT_MAX_RETRIES)
                .createdAt(LocalDateTime.now())
                .build();

        Notification savedNotification = notificationRepository.save(notification);
        log.info("Order notification created with ID: {}", savedNotification.getId());

        // Dispatch notification asynchronously
        notificationDispatcher.dispatch(savedNotification);

        return toDTO(savedNotification);
    }

    public NotificationDTO getNotificationById(String notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", notificationId));
        return toDTO(notification);
    }

    public PageResponse<NotificationDTO> getCustomerNotifications(UUID customerId, String type, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Notification> notificationPage;
        if (type != null && !type.isEmpty()) {
            notificationPage = notificationRepository.findByCustomerIdAndNotificationType(customerId, type, pageable);
        } else {
            notificationPage = notificationRepository.findByCustomerId(customerId, pageable);
        }

        return PageResponse.<NotificationDTO>builder()
                .content(notificationPage.getContent().stream().map(this::toDTO).toList())
                .pageNumber(notificationPage.getNumber())
                .pageSize(notificationPage.getSize())
                .totalElements(notificationPage.getTotalElements())
                .totalPages(notificationPage.getTotalPages())
                .first(notificationPage.isFirst())
                .last(notificationPage.isLast())
                .build();
    }

    public NotificationDTO markAsSent(String notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", notificationId));

        notification.markAsSent();
        Notification savedNotification = notificationRepository.save(notification);
        log.info("Notification {} marked as sent", notificationId);

        return toDTO(savedNotification);
    }

    public NotificationDTO markAsFailed(String notificationId, String errorMessage) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", notificationId));

        notification.markAsFailed(errorMessage);
        Notification savedNotification = notificationRepository.save(notification);
        log.info("Notification {} marked as failed: {}", notificationId, errorMessage);

        return toDTO(savedNotification);
    }

    private NotificationDTO toDTO(Notification notification) {
        return NotificationDTO.builder()
                .id(notification.getId())
                .eventId(notification.getEventId())
                .customerId(notification.getCustomerId())
                .notificationType(notification.getNotificationType())
                .channel(notification.getChannel())
                .recipient(notification.getRecipient())
                .subject(notification.getSubject())
                .body(notification.getBody())
                .templateVariables(notification.getTemplateVariables())
                .status(notification.getStatus())
                .errorMessage(notification.getErrorMessage())
                .retryCount(notification.getRetryCount())
                .scheduledAt(notification.getScheduledAt())
                .sentAt(notification.getSentAt())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
