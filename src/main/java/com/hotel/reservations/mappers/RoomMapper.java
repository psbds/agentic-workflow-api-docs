package com.hotel.reservations.mappers;

import com.hotel.reservations.domain.dto.RoomDto;
import com.hotel.reservations.domain.entities.Room;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class RoomMapper {

    public RoomDto toDto(Room entity) {
        return new RoomDto(
                entity.getId(),
                entity.getRoomNumber(),
                entity.getRoomType().name(),
                entity.getPricePerNight(),
                entity.getMaxOccupancy(),
                entity.getDescription(),
                entity.isAvailable(),
                entity.getFloorNumber(),
                entity.getHotel() != null ? entity.getHotel().getId() : null,
                entity.getHotel() != null ? entity.getHotel().getName() : null
        );
    }

    public List<RoomDto> toDtoList(List<Room> entities) {
        return entities.stream().map(this::toDto).toList();
    }
}
