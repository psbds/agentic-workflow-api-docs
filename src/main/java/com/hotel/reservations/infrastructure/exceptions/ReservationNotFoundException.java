package com.hotel.reservations.infrastructure.exceptions;

public class ReservationNotFoundException extends HotelException {

    public ReservationNotFoundException(String identifier) {
        super("RESERVATION_NOT_FOUND", "Reservation not found with identifier: " + identifier);
    }
}
