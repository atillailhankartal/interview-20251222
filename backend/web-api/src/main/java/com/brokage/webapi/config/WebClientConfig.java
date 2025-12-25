package com.brokage.webapi.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${services.order-service.url}")
    private String orderServiceUrl;

    @Value("${services.asset-service.url}")
    private String assetServiceUrl;

    @Value("${services.customer-service.url}")
    private String customerServiceUrl;

    @Value("${services.notification-service.url}")
    private String notificationServiceUrl;

    @Value("${services.audit-service.url}")
    private String auditServiceUrl;

    @Bean
    public WebClient orderServiceClient() {
        return WebClient.builder()
                .baseUrl(orderServiceUrl)
                .build();
    }

    @Bean
    public WebClient assetServiceClient() {
        return WebClient.builder()
                .baseUrl(assetServiceUrl)
                .build();
    }

    @Bean
    public WebClient customerServiceClient() {
        return WebClient.builder()
                .baseUrl(customerServiceUrl)
                .build();
    }

    @Bean
    public WebClient notificationServiceClient() {
        return WebClient.builder()
                .baseUrl(notificationServiceUrl)
                .build();
    }

    @Bean
    public WebClient auditServiceClient() {
        return WebClient.builder()
                .baseUrl(auditServiceUrl)
                .build();
    }
}
