package com.hotel.reservations.infrastructure.exceptions;

public class InvalidDateRangeException extends HotelException {

    public InvalidDateRangeException() {
        super("INVALID_DATE_RANGE", "The provided date range is invalid");
    }

    public InvalidDateRangeException(String message) {
        super("INVALID_DATE_RANGE", message);
    }
}
