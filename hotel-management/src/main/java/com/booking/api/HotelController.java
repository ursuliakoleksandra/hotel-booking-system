package com.booking.api;

import com.booking.api.dto.HotelDTO;
import com.booking.application.HotelService;
import com.booking.domain.Hotel;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/hotels") // ПУНКТ 4: Версійність v1
@RequiredArgsConstructor
public class HotelController {
    private final HotelService hotelService;
    private final HotelApiMapper hotelApiMapper;

    @GetMapping("/{id}")
    public HotelDTO getById(@PathVariable Long id) {
        Hotel hotel = hotelService.getHotelById(id); // Переконайся, що такий метод є в HotelService
        return hotelApiMapper.toDto(hotel);
    }

    @PostMapping
    public HotelDTO create(@Valid @RequestBody HotelDTO dto) { // ПУНКТ 1: @Valid
        Hotel hotel = hotelApiMapper.toDomain(dto);
        Hotel saved = hotelService.createHotel(hotel);
        return hotelApiMapper.toDto(saved);
    }

    @GetMapping
    public List<HotelDTO> getAll() {
        return hotelService.getAllHotels().stream()
                .map(hotelApiMapper::toDto)
                .toList();
    }
}
