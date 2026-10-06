package com.booking.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class HotelDTO {
    private Long id;

    @NotBlank(message = "Назва готелю обов'язкова")
    private String name;

    @NotBlank(message = "Адреса готелю обов'язкова")
    private String address;

    @NotNull(message = "Рейтинг не може бути порожнім")
    @Min(value = 1, message = "Рейтинг має бути мінімум 1")
    @Max(value = 5, message = "Рейтинг має бути максимум 5")
    private Integer rating;
}