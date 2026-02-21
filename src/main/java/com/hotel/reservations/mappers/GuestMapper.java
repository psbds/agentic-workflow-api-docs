package com.hotel.reservations.mappers;

import com.hotel.reservations.domain.dto.GuestDto;
import com.hotel.reservations.domain.entities.Guest;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class GuestMapper {

    public GuestDto toDto(Guest entity) {
        return new GuestDto(
                entity.getId(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getEmail(),
                entity.getPhoneNumber(),
                entity.getDocumentType(),
                entity.getDocumentNumber(),
                entity.getNationality()
        );
    }

    public List<GuestDto> toDtoList(List<Guest> entities) {
        return entities.stream().map(this::toDto).toList();
    }
}
