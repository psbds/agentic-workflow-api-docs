package com.hotel.reservations.domain.dto;

import com.hotel.reservations.domain.enums.PaymentStatus;
import com.hotel.reservations.domain.enums.ReservationStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReservationDto(
        Long id,
        String confirmationCode,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        int numberOfGuests,
        BigDecimal totalPrice,
        ReservationStatus status,
        PaymentStatus paymentStatus,
        String specialRequests,
        boolean weatherChecked,
        String weatherSummary,
        Long guestId,
        String guestName,
        Long roomId,
        String roomNumber,
        String hotelName
) {
}
