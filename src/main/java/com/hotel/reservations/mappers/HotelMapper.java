package com.hotel.reservations.mappers;

import com.hotel.reservations.domain.dto.HotelDto;
import com.hotel.reservations.domain.entities.Hotel;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class HotelMapper {

    public HotelDto toDto(Hotel entity) {
        return new HotelDto(
                entity.getId(),
                entity.getName(),
                entity.getAddress(),
                entity.getCity(),
                entity.getCountry(),
                entity.getStarRating(),
                entity.getDescription(),
                entity.getLatitude(),
                entity.getLongitude(),
                entity.getPhoneNumber(),
                entity.getEmail(),
                entity.getRooms() != null ? entity.getRooms().size() : 0
        );
    }

    public List<HotelDto> toDtoList(List<Hotel> entities) {
        return entities.stream().map(this::toDto).toList();
    }
}
