package com.booking.domain;
import java.util.List;
import java.util.Optional;

public interface HotelRepository {
    List<Hotel> findAll();
    Optional<Hotel> findById(Long id);
    Hotel save(Hotel hotel);
}
