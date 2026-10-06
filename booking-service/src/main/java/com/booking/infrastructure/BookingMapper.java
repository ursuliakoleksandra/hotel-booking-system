package com.booking.infrastructure;

import com.booking.domain.Booking;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface BookingMapper {
    BookingEntity toEntity(Booking domain);
    Booking toDomain(BookingEntity entity);
}