package com.brokage.order.kafka;

import com.brokage.order.entity.OutboxEvent;
import com.brokage.order.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    private static final int MAX_RETRIES = 3;
    private static final String ORDER_EVENTS_TOPIC = "order-events";

    @Scheduled(fixedDelay = 1000) // Poll every second
    @Transactional
    public void publishOutboxEvents() {
        List<OutboxEvent> events = outboxEventRepository.findUnprocessedEvents(MAX_RETRIES);

        for (OutboxEvent event : events) {
            try {
                String key = event.getAggregateId().toString();
                String value = event.getPayload();

                // Use blocking .get() to ensure message is sent before marking as processed
                kafkaTemplate.send(ORDER_EVENTS_TOPIC, key, value).get();

                // Mark as processed only after successful send
                event.markAsProcessed();
                outboxEventRepository.save(event);

                log.info("Published outbox event: type={}, aggregateId={}",
                        event.getEventType(), event.getAggregateId());

            } catch (Exception e) {
                log.error("Error publishing outbox event {}: {}", event.getId(), e.getMessage());
                event.markAsFailed(e.getMessage());
                outboxEventRepository.save(event);
            }
        }
    }
}
