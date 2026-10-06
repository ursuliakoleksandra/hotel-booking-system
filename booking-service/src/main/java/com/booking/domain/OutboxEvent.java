package com.booking.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "outbox")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OutboxEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String aggregateId;
    private String type;

    @Column(columnDefinition = "TEXT")
    private String payload;

    private boolean processed = false;
}
