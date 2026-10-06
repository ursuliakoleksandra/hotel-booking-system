package com.booking.infrastructure;
import com.booking.domain.Hotel;
import com.booking.domain.HotelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor

public class HotelRepositoryImpl implements HotelRepository {
    private final JpaHotelRepository jpaRepository;
    private final HotelMapper hotelMapper;

    @Override
    public List<Hotel> findAll() {
        return jpaRepository.findAll().stream()
                .map(hotelMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Hotel> findById(Long id) {
        return jpaRepository.findById(id)
                .map(hotelMapper::toDomain);
    }

    @Override
    public Hotel save(Hotel hotel) {
        HotelEntity entity = hotelMapper.toEntity(hotel);
        HotelEntity saved = jpaRepository.save(entity);
        return hotelMapper.toDomain(saved);
    }
}
