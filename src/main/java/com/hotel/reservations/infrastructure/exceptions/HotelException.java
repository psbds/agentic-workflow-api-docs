package com.hotel.reservations.infrastructure.exceptions;

public class HotelException extends RuntimeException {

    private final String errorCode;

    public HotelException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
