package com.hotel.reservations.mappers;

import com.hotel.reservations.domain.dto.ReservationDto;
import com.hotel.reservations.domain.entities.Reservation;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class ReservationMapper {

    public ReservationDto toDto(Reservation entity) {
        return new ReservationDto(
                entity.getId(),
                entity.getConfirmationCode(),
                entity.getCheckInDate(),
                entity.getCheckOutDate(),
                entity.getNumberOfGuests(),
                entity.getTotalPrice(),
                entity.getStatus().name(),
                entity.getPaymentStatus().name(),
                entity.getSpecialRequests(),
                entity.isWeatherChecked(),
                entity.getWeatherSummary(),
                entity.getGuest() != null ? entity.getGuest().getId() : null,
                entity.getGuest() != null ? entity.getGuest().getFirstName() + " " + entity.getGuest().getLastName() : null,
                entity.getRoom() != null ? entity.getRoom().getId() : null,
                entity.getRoom() != null ? entity.getRoom().getRoomNumber() : null,
                entity.getRoom() != null && entity.getRoom().getHotel() != null ? entity.getRoom().getHotel().getName() : null
        );
    }

    public List<ReservationDto> toDtoList(List<Reservation> entities) {
        return entities.stream().map(this::toDto).toList();
    }
}
