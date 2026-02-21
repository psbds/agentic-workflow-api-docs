package com.hotel.reservations.infrastructure.exceptions;

public class WeatherCheckFailedException extends HotelException {

    public WeatherCheckFailedException(String message) {
        super("WEATHER_CHECK_FAILED", message);
    }
}
