package com.brokage.notification.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "notification")
public class NotificationProperties {

    private EmailProperties email = new EmailProperties();
    private SmsProperties sms = new SmsProperties();

    @Data
    public static class EmailProperties {
        private boolean enabled = false;
        private String from = "noreply@brokage.com";
        private String fromName = "Brokage Trading Platform";
    }

    @Data
    public static class SmsProperties {
        private boolean enabled = false;
        private String provider = "LOG"; // LOG, TWILIO, etc.
    }
}
