package com.booking.domain;
import lombok.*;

@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class User {
    private Long id;
    private String fullName;
    private String email;
}