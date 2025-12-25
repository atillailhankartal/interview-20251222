package com.brokage.notification.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
public class AsyncConfig {
    // Default async configuration
    // Spring will use SimpleAsyncTaskExecutor by default
    // For production, consider configuring a ThreadPoolTaskExecutor
}
