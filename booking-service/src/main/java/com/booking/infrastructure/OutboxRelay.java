package com.booking.infrastructure;

import com.booking.domain.OutboxEvent;
import com.booking.domain.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxRelay {
    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void processOutbox() {
        var events = outboxRepository.findByProcessedFalse();
        for (OutboxEvent event : events) {
            try {
                kafkaTemplate.send("booking-events", event.getPayload());
                event.setProcessed(true);
                outboxRepository.save(event);
                log.info("Подія {} відправлена в Kafka", event.getAggregateId());
            } catch (Exception e) {
                log.error("Помилка відправки в Kafka для події {}", event.getAggregateId());
            }
        }
    }
}