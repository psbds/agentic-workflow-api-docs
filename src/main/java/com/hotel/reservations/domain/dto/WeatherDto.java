package com.hotel.reservations.domain.dto;

import java.time.LocalDate;

public record WeatherDto(
        double temperature,
        double windSpeed,
        String weatherDescription,
        String locationName,
        LocalDate forecastDate,
        boolean isSuitableForTravel
) {
}
