package com.brokage.notification.sender;

import com.brokage.notification.config.NotificationProperties;
import com.brokage.notification.document.Notification;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailSenderService {

    private final JavaMailSender mailSender;
    private final NotificationProperties properties;

    public void sendEmail(Notification notification) {
        if (!properties.getEmail().isEnabled()) {
            log.info("[EMAIL-DISABLED] Would send email to {}: {}",
                    notification.getRecipient(), notification.getSubject());
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String recipient = notification.getRecipient();
            if (recipient == null || recipient.isEmpty()) {
                recipient = notification.getCustomerEmail();
            }
            if (recipient == null || recipient.isEmpty()) {
                // Fallback to customer ID based email for demo
                recipient = notification.getCustomerId() + "@demo.brokage.com";
            }

            helper.setFrom(properties.getEmail().getFrom(), properties.getEmail().getFromName());
            helper.setTo(recipient);
            helper.setSubject(notification.getSubject() != null ? notification.getSubject() : "Brokage Notification");

            String body = buildEmailBody(notification);
            helper.setText(body, true); // true = HTML

            mailSender.send(message);
            log.info("[EMAIL-SENT] Sent email to {}: {}", recipient, notification.getSubject());

        } catch (MessagingException e) {
            log.error("[EMAIL-ERROR] Failed to send email: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send email", e);
        } catch (Exception e) {
            log.error("[EMAIL-ERROR] Unexpected error sending email: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    private String buildEmailBody(Notification notification) {
        if (notification.getBody() != null && !notification.getBody().isEmpty()) {
            return wrapInHtmlTemplate(notification.getBody());
        }

        // Build from template variables
        StringBuilder body = new StringBuilder();
        body.append("<p>Dear Customer,</p>");

        if (notification.getTemplateVariables() != null) {
            var vars = notification.getTemplateVariables();

            if (vars.containsKey("orderStatus")) {
                body.append("<p>Your order has been <strong>")
                    .append(vars.get("orderStatus"))
                    .append("</strong>.</p>");

                body.append("<table border='1' cellpadding='5' style='border-collapse: collapse;'>");
                body.append("<tr><th>Order ID</th><td>").append(vars.getOrDefault("orderId", "N/A")).append("</td></tr>");
                body.append("<tr><th>Asset</th><td>").append(vars.getOrDefault("assetName", "N/A")).append("</td></tr>");
                body.append("<tr><th>Side</th><td>").append(vars.getOrDefault("orderSide", "N/A")).append("</td></tr>");
                body.append("<tr><th>Amount</th><td>").append(vars.getOrDefault("amount", "N/A")).append("</td></tr>");
                body.append("</table>");
            }
        }

        body.append("<p>Thank you for using Brokage Trading Platform.</p>");
        body.append("<hr><p style='font-size: 12px; color: #666;'>This is an automated notification. Please do not reply.</p>");

        return wrapInHtmlTemplate(body.toString());
    }

    private String wrapInHtmlTemplate(String content) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    table { margin: 15px 0; }
                    th { background-color: #f5f5f5; text-align: left; }
                </style>
            </head>
            <body>
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <div style="background: linear-gradient(135deg, #ff6600, #ff8533); color: white; padding: 20px; text-align: center;">
                        <h1 style="margin: 0;">Brokage Trading</h1>
                    </div>
                    <div style="padding: 20px; background: #fff;">
                        %s
                    </div>
                    <div style="padding: 10px; background: #f5f5f5; text-align: center; font-size: 12px; color: #666;">
                        &copy; 2025 Brokage Trading Platform. All rights reserved.
                    </div>
                </div>
            </body>
            </html>
            """.formatted(content);
    }
}
