package com.booking.api;

import com.booking.api.dto.BookingDTO;
import com.booking.application.BookingService;
import com.booking.domain.Booking;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {
    private final BookingService bookingService;
    private final BookingApiMapper mapper;

    @PostMapping
    public BookingDTO create(@Valid @RequestBody BookingDTO dto) {
        Booking domain = mapper.toDomain(dto);
        Booking saved = bookingService.createBooking(domain);
        return mapper.toDto(saved);
    }

    @GetMapping
    public List<BookingDTO> getAll() {
        return bookingService.getAllBookings().stream()
                .map(mapper::toDto)
                .toList();
    }
}
