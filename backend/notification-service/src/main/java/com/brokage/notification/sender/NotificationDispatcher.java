package com.brokage.notification.sender;

import com.brokage.notification.document.Notification;
import com.brokage.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationDispatcher {

    private final EmailSenderService emailSenderService;
    private final SmsSenderService smsSenderService;
    private final NotificationRepository notificationRepository;

    @Async
    public void dispatch(Notification notification) {
        log.info("Dispatching notification {} via channel {}", notification.getId(), notification.getChannel());

        try {
            String channel = notification.getChannel();
            if (channel == null) {
                channel = "EMAIL"; // Default to email
            }

            switch (channel.toUpperCase()) {
                case "EMAIL" -> emailSenderService.sendEmail(notification);
                case "SMS" -> smsSenderService.sendSms(notification);
                case "PUSH" -> sendPushNotification(notification);
                case "IN_APP" -> sendInAppNotification(notification);
                default -> {
                    log.warn("Unknown notification channel: {}, defaulting to EMAIL", channel);
                    emailSenderService.sendEmail(notification);
                }
            }

            // Mark as sent
            notification.markAsSent();
            notificationRepository.save(notification);
            log.info("Notification {} sent successfully via {}", notification.getId(), channel);

        } catch (Exception e) {
            log.error("Failed to dispatch notification {}: {}", notification.getId(), e.getMessage(), e);
            notification.markAsFailed(e.getMessage());
            notificationRepository.save(notification);
        }
    }

    private void sendPushNotification(Notification notification) {
        // Push notification implementation would go here (Firebase, APNs, etc.)
        log.info("[PUSH] Would send push notification to customer {}: {}",
                notification.getCustomerId(), notification.getBody());
    }

    private void sendInAppNotification(Notification notification) {
        // In-app notification is already saved to MongoDB
        // WebSocket would broadcast to connected clients
        log.info("[IN_APP] In-app notification saved for customer {}: {}",
                notification.getCustomerId(), notification.getBody());
    }
}
