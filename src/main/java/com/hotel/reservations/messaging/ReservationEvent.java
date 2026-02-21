package com.hotel.reservations.messaging;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ReservationEvent(
    EventType eventType,
    Long reservationId,
    String confirmationCode,
    Long guestId,
    String guestEmail,
    Long roomId,
    LocalDate checkInDate,
    LocalDate checkOutDate,
    BigDecimal totalPrice,
    String status,
    LocalDateTime timestamp
) {
    public enum EventType {
        CREATED, CONFIRMED, CANCELLED, CHECKED_IN, CHECKED_OUT, UPDATED, EXPIRED
    }
}
