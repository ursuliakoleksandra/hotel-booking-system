package com.booking.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
public class FeignInterceptor implements RequestInterceptor {
    @Override
    public void apply(RequestTemplate template) {
        // Перевіряємо, чи вже є ID, якщо ні - створюємо новий
        String correlationId = MDC.get("correlationId");
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
        }
        // Додаємо заголовок, який піде в інший сервіс
        template.header("X-Correlation-ID", correlationId);
    }
}