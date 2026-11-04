package com.booking.infrastructure;

import com.booking.domain.OutboxEvent;
import com.booking.domain.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxRelay {
    private static final long SEND_TIMEOUT_SECONDS = 10;

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void processOutbox() {
        var events = outboxRepository.findByProcessedFalse();
        for (OutboxEvent event : events) {
            try {
                // Чекаємо підтвердження від брокера: позначаємо подію як оброблену лише після успішної відправки
                kafkaTemplate.send("booking-events", event.getAggregateId(), event.getPayload())
                        .get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                event.setProcessed(true);
                outboxRepository.save(event);
                log.info("Подія {} відправлена в Kafka", event.getAggregateId());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                log.error("Помилка відправки в Kafka для події {}: {}", event.getAggregateId(), e.getMessage());
                // Kafka недоступна: решту подій спробуємо в наступному циклі
                return;
            }
        }
    }
}
