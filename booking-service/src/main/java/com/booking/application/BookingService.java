package com.booking.application;

import com.booking.client.HotelClient;
import com.booking.domain.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingService {

    private static final Logger logger = LoggerFactory.getLogger(BookingService.class);
    private final BookingRepository bookingRepository;
    private final OutboxRepository outboxRepository; // Додано
    private final HotelClient hotelClient;
    private final ObjectMapper objectMapper; // Додано для JSON

    @Transactional // КРИТИЧНО ВАЖЛИВО: забезпечує атомарність (Пункт 2 завдання)
    public Booking createBooking(Booking booking) {
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);

        try {
            logger.info("[START] Спроба створення бронювання. Correlation ID: {}", correlationId);

            // 1. Синхронна перевірка готелю через Feign
            logger.info("Запит до Hotel Service для перевірки готелю ID: {}", booking.getHotelId());
            Object hotel = hotelClient.getHotelById(booking.getHotelId());

            if (hotel == null) {
                logger.warn("Hotel Service повернув порожній об'єкт (Fallback)");
                throw new RuntimeException("Готель недоступний або не знайдений");
            }

            // 2. Збереження бронювання
            logger.info("Готель підтверджено. Збереження бронювання в БД...");
            Booking savedBooking = bookingRepository.save(booking);

            // 3. Реалізація Transactional Outbox (Пункт 2 завдання)
            // Зберігаємо подію в ту ж БД, що і бронювання
            OutboxEvent event = OutboxEvent.builder()
                    .aggregateId(savedBooking.getId().toString())
                    .type("BOOKING_CREATED")
                    .payload(objectMapper.writeValueAsString(savedBooking)) // Конвертуємо об'єкт у JSON
                    .processed(false)
                    .build();

            outboxRepository.save(event);
            logger.info("Подія збережена в Outbox таблицю. Очікує відправки в Kafka.");

            return savedBooking;

        } catch (Exception e) {
            logger.error("Помилка при створенні бронювання: {}", e.getMessage());
            throw new RuntimeException(e.getMessage());
        } finally {
            logger.info("[END] Обробка запиту завершена.");
            MDC.remove("correlationId");
        }
    }

    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }
}