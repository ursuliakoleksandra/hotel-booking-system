package com.booking.client;


import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

@FeignClient(name = "hotel-service", url = "http://localhost:8081/api/v1/hotels")
public interface HotelClient {

    @GetMapping("/{id}")
    @CircuitBreaker(name = "hotelService", fallbackMethod = "fallbackGetHotel")
    @Retry(name = "hotelService")
    Object getHotelById(@PathVariable("id") Long id);

    default Object fallbackGetHotel(Long id, Throwable t) {
        System.out.println("Circuit Breaker спрацював! Сервіс готелів недоступний.");
        return null;
    }
}