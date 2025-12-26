package com.brokage.processor.kafka;

import com.brokage.processor.entity.OutboxEvent;
import com.brokage.processor.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    private static final int BATCH_SIZE = 100;
    private static final int MAX_RETRIES = 3;
    private static final String ORDER_EVENTS_TOPIC = "order-events";

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> events = outboxEventRepository.findRetryableEvents(
                MAX_RETRIES,
                PageRequest.of(0, BATCH_SIZE)
        );

        if (events.isEmpty()) {
            return;
        }

        log.debug("Publishing {} outbox events", events.size());

        for (OutboxEvent event : events) {
            try {
                kafkaTemplate.send(
                        ORDER_EVENTS_TOPIC,
                        event.getPartitionKey(),
                        event.getPayload()
                ).get();

                event.markAsProcessed();
                outboxEventRepository.save(event);

                log.debug("Published event {} to topic {}", event.getId(), ORDER_EVENTS_TOPIC);
            } catch (Exception e) {
                log.error("Failed to publish event {}: {}", event.getId(), e.getMessage());
                event.markAsFailed(e.getMessage());
                outboxEventRepository.save(event);
            }
        }
    }

    public long getPendingEventCount() {
        return outboxEventRepository.countByProcessedFalse();
    }
}
