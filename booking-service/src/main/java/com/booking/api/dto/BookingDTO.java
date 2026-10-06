package com.booking.api.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

@Data
public class BookingDTO {
    private Long id;

    @NotNull(message = "ID користувача обов'язкове")
    private Long userId;

    @NotNull(message = "ID готелю обов'язкове")
    private Long hotelId;

    @NotNull(message = "Дата заїзду обов'язкова")
    @FutureOrPresent(message = "Дата заїзду не може бути в минулому")
    private LocalDate checkIn;

    @NotNull(message = "Дата виїзду обов'язкова")
    private LocalDate checkOut;
}
