package com.brokage.notification.sender;

import com.brokage.notification.config.NotificationProperties;
import com.brokage.notification.document.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmsSenderService {

    private final NotificationProperties properties;

    public void sendSms(Notification notification) {
        if (!properties.getSms().isEnabled()) {
            log.info("[SMS-DISABLED] Would send SMS to customer {}: {}",
                    notification.getCustomerId(), notification.getBody());
            return;
        }

        String provider = properties.getSms().getProvider();

        switch (provider.toUpperCase()) {
            case "LOG" -> sendViaLog(notification);
            case "TWILIO" -> sendViaTwilio(notification);
            default -> {
                log.warn("[SMS] Unknown provider: {}, falling back to LOG", provider);
                sendViaLog(notification);
            }
        }
    }

    private void sendViaLog(Notification notification) {
        String phoneNumber = notification.getRecipient();
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            phoneNumber = "+90555" + notification.getCustomerId().toString().substring(0, 7).replaceAll("-", "");
        }

        String message = buildSmsMessage(notification);

        log.info("╔══════════════════════════════════════════════════════════════╗");
        log.info("║                    SMS NOTIFICATION                          ║");
        log.info("╠══════════════════════════════════════════════════════════════╣");
        log.info("║ To:      {}                                  ", phoneNumber);
        log.info("║ Message: {}                                  ", message);
        log.info("║ Status:  SENT (via LOG provider)                             ║");
        log.info("╚══════════════════════════════════════════════════════════════╝");
    }

    private void sendViaTwilio(Notification notification) {
        // Twilio integration would go here
        // For now, just log as if it was sent
        log.info("[SMS-TWILIO] Would send via Twilio to {}: {}",
                notification.getRecipient(), notification.getBody());

        // In production:
        // TwilioRestClient client = new TwilioRestClient(accountSid, authToken);
        // Message.creator(toPhone, fromPhone, message).create();

        throw new UnsupportedOperationException("Twilio integration not implemented. Use LOG provider for demo.");
    }

    private String buildSmsMessage(Notification notification) {
        if (notification.getBody() != null && !notification.getBody().isEmpty()) {
            // Truncate for SMS (160 char limit)
            String body = notification.getBody();
            if (body.length() > 150) {
                body = body.substring(0, 147) + "...";
            }
            return body;
        }

        // Build from template variables
        var vars = notification.getTemplateVariables();
        if (vars != null && vars.containsKey("orderStatus")) {
            return String.format("Brokage: Your %s order for %s %s has been %s. Order ID: %s",
                    vars.getOrDefault("orderSide", ""),
                    vars.getOrDefault("amount", ""),
                    vars.getOrDefault("assetName", ""),
                    vars.getOrDefault("orderStatus", ""),
                    vars.getOrDefault("orderId", "").toString().substring(0, 8));
        }

        return "Brokage Trading: " + notification.getNotificationType();
    }
}
