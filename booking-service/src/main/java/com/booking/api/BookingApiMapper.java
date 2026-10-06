package com.booking.api;

import com.booking.api.dto.BookingDTO;
import com.booking.domain.Booking;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface BookingApiMapper {
    BookingDTO toDto(Booking domain);
    Booking toDomain(BookingDTO dto);
}