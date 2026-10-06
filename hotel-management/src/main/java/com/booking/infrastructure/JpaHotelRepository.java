package com.booking.infrastructure;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaHotelRepository extends JpaRepository<HotelEntity, Long> {
}
