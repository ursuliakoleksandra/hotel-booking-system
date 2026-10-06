package com.booking.api;

import com.booking.api.dto.HotelDTO;
import com.booking.domain.Hotel;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface HotelApiMapper {
    HotelDTO toDto(Hotel hotel);
    Hotel toDomain(HotelDTO dto);
}