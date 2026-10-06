package com.booking.infrastructure;

import com.booking.domain.Hotel;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface HotelMapper {
    HotelEntity toEntity(Hotel domain);
    Hotel toDomain(HotelEntity entity);
}