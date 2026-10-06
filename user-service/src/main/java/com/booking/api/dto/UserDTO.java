package com.booking.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserDTO {
    private Long id;

    @NotBlank(message = "Ім'я користувача є обов'язковим")
    @Size(min = 2, max = 50, message = "Ім'я має бути від 2 до 50 символів")
    private String fullName;

    @NotBlank(message = "Email не може бути порожнім")
    @Email(message = "Некоректний формат Email")
    private String email;
}
