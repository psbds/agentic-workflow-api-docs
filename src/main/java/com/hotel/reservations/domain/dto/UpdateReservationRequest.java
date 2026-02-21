package com.hotel.reservations.domain.dto;

import java.time.LocalDate;

public record UpdateReservationRequest(
        LocalDate checkInDate,
        LocalDate checkOutDate,
        Integer numberOfGuests,
        String specialRequests
) {
}
