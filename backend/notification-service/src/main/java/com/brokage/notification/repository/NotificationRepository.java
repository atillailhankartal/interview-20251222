package com.brokage.notification.repository;

import com.brokage.notification.document.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRepository extends MongoRepository<Notification, String> {

    Page<Notification> findByCustomerId(UUID customerId, Pageable pageable);

    Page<Notification> findByCustomerIdAndNotificationType(UUID customerId, String notificationType, Pageable pageable);

    List<Notification> findByStatus(String status);

    @Query("{'status': 'PENDING', 'scheduledAt': {$lte: ?0}}")
    List<Notification> findPendingNotificationsToSend(LocalDateTime now);

    @Query("{'status': 'FAILED', 'retryCount': {$lt: ?0}}")
    List<Notification> findFailedNotificationsToRetry(int maxRetries);

    Optional<Notification> findByEventId(UUID eventId);

    long countByCustomerIdAndStatus(UUID customerId, String status);
}
