package com.brokage.notification.service;

import com.brokage.common.exception.ResourceNotFoundException;
import com.brokage.notification.document.Notification;
import com.brokage.notification.dto.NotificationDTO;
import com.brokage.notification.dto.SendNotificationRequest;
import com.brokage.notification.repository.NotificationRepository;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.brokage.notification.sender.NotificationDispatcher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationDispatcher notificationDispatcher;

    @InjectMocks
    private NotificationService notificationService;

    private UUID customerId;
    private UUID eventId;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        eventId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("Create Notification Tests")
    class CreateNotificationTests {

        @Test
        @DisplayName("Should create notification successfully")
        void shouldCreateNotificationSuccessfully() {
            // Given
            SendNotificationRequest request = SendNotificationRequest.builder()
                    .customerId(customerId)
                    .templateCode("ORDER_CREATED")
                    .channel("EMAIL")
                    .templateVariables(Map.of("orderId", "123"))
                    .build();

            Notification savedNotification = createNotification("notif-1", customerId, "ORDER_CREATED");

            when(notificationRepository.findByEventId(any())).thenReturn(Optional.empty());
            when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);

            // When
            NotificationDTO result = notificationService.createNotification(request, "ORDER_CREATED", eventId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getNotificationType()).isEqualTo("ORDER_CREATED");
            verify(notificationRepository).save(any(Notification.class));
        }

        @Test
        @DisplayName("Should return existing notification for duplicate eventId")
        void shouldReturnExistingNotificationForDuplicateEventId() {
            // Given
            SendNotificationRequest request = SendNotificationRequest.builder()
                    .customerId(customerId)
                    .templateCode("ORDER_CREATED")
                    .channel("EMAIL")
                    .build();

            Notification existingNotification = createNotification("notif-1", customerId, "ORDER_CREATED");
            existingNotification.setEventId(eventId);

            when(notificationRepository.findByEventId(eventId)).thenReturn(Optional.of(existingNotification));

            // When
            NotificationDTO result = notificationService.createNotification(request, "ORDER_CREATED", eventId);

            // Then
            assertThat(result).isNotNull();
            verify(notificationRepository, never()).save(any(Notification.class));
        }
    }

    @Nested
    @DisplayName("Create Order Notification Tests")
    class CreateOrderNotificationTests {

        @Test
        @DisplayName("Should create order notification successfully")
        void shouldCreateOrderNotificationSuccessfully() {
            // Given
            UUID orderId = UUID.randomUUID();
            Notification savedNotification = createNotification("notif-1", customerId, "ORDER_CREATED");

            when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);

            // When
            NotificationDTO result = notificationService.createOrderNotification(
                    customerId, orderId, "CREATED", "AAPL", "BUY", "100");

            // Then
            assertThat(result).isNotNull();
            verify(notificationRepository).save(any(Notification.class));
        }
    }

    @Nested
    @DisplayName("Get Notification Tests")
    class GetNotificationTests {

        @Test
        @DisplayName("Should get notification by ID")
        void shouldGetNotificationById() {
            // Given
            String notificationId = "notif-123";
            Notification notification = createNotification(notificationId, customerId, "ORDER_MATCHED");

            when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));

            // When
            NotificationDTO result = notificationService.getNotificationById(notificationId);

            // Then
            assertThat(result.getId()).isEqualTo(notificationId);
            assertThat(result.getNotificationType()).isEqualTo("ORDER_MATCHED");
        }

        @Test
        @DisplayName("Should throw exception when notification not found")
        void shouldThrowExceptionWhenNotificationNotFound() {
            // Given
            String notificationId = "non-existent";
            when(notificationRepository.findById(notificationId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> notificationService.getNotificationById(notificationId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Get Customer Notifications Tests")
    class GetCustomerNotificationsTests {

        @Test
        @DisplayName("Should get customer notifications with pagination")
        void shouldGetCustomerNotificationsWithPagination() {
            // Given
            List<Notification> notifications = List.of(
                    createNotification("notif-1", customerId, "ORDER_CREATED"),
                    createNotification("notif-2", customerId, "ORDER_MATCHED")
            );
            Page<Notification> notificationPage = new PageImpl<>(notifications);

            when(notificationRepository.findByCustomerId(eq(customerId), any(Pageable.class)))
                    .thenReturn(notificationPage);

            // When
            var result = notificationService.getCustomerNotifications(customerId, null, 0, 10);

            // Then
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should filter notifications by type")
        void shouldFilterNotificationsByType() {
            // Given
            List<Notification> notifications = List.of(
                    createNotification("notif-1", customerId, "ORDER_MATCHED")
            );
            Page<Notification> notificationPage = new PageImpl<>(notifications);

            when(notificationRepository.findByCustomerIdAndNotificationType(
                    eq(customerId), eq("ORDER_MATCHED"), any(Pageable.class)))
                    .thenReturn(notificationPage);

            // When
            var result = notificationService.getCustomerNotifications(customerId, "ORDER_MATCHED", 0, 10);

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getNotificationType()).isEqualTo("ORDER_MATCHED");
        }
    }

    @Nested
    @DisplayName("Mark Notification Status Tests")
    class MarkNotificationStatusTests {

        @Test
        @DisplayName("Should mark notification as sent")
        void shouldMarkNotificationAsSent() {
            // Given
            String notificationId = "notif-1";
            Notification notification = createNotification(notificationId, customerId, "ORDER_CREATED");

            when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));
            when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
                Notification n = inv.getArgument(0);
                n.markAsSent();
                return n;
            });

            // When
            NotificationDTO result = notificationService.markAsSent(notificationId);

            // Then
            assertThat(result.getStatus()).isEqualTo(Notification.NotificationStatus.SENT.name());
        }

        @Test
        @DisplayName("Should mark notification as failed")
        void shouldMarkNotificationAsFailed() {
            // Given
            String notificationId = "notif-1";
            String errorMessage = "SMTP server not reachable";
            Notification notification = createNotification(notificationId, customerId, "ORDER_CREATED");

            when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));
            when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
                Notification n = inv.getArgument(0);
                n.markAsFailed(errorMessage);
                return n;
            });

            // When
            NotificationDTO result = notificationService.markAsFailed(notificationId, errorMessage);

            // Then
            assertThat(result.getStatus()).isEqualTo(Notification.NotificationStatus.FAILED.name());
            assertThat(result.getErrorMessage()).isEqualTo(errorMessage);
        }
    }

    private Notification createNotification(String id, UUID customerId, String type) {
        return Notification.builder()
                .id(id)
                .customerId(customerId)
                .notificationType(type)
                .channel("EMAIL")
                .status(Notification.NotificationStatus.PENDING.name())
                .retryCount(0)
                .maxRetries(3)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
