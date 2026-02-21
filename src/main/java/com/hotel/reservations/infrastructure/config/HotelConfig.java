package com.hotel.reservations.infrastructure.config;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class HotelConfig {

    @ConfigProperty(name = "hotel.reservation.max-days")
    int maxReservationDays;

    @ConfigProperty(name = "hotel.reservation.max-rooms-per-booking")
    int maxRoomsPerBooking;

    @ConfigProperty(name = "hotel.reservation.cancellation-hours-before")
    int cancellationHoursBefore;

    @ConfigProperty(name = "hotel.cache.ttl-minutes")
    int cacheTtlMinutes;

    @ConfigProperty(name = "hotel.weather.max-wind-speed-kmh")
    double maxWindSpeed;

    @ConfigProperty(name = "hotel.weather.min-temperature-celsius")
    double minTemperature;

    @ConfigProperty(name = "hotel.weather.max-temperature-celsius")
    double maxTemperature;

    public int getMaxReservationDays() {
        return maxReservationDays;
    }

    public int getMaxRoomsPerBooking() {
        return maxRoomsPerBooking;
    }

    public int getCancellationHoursBefore() {
        return cancellationHoursBefore;
    }

    public int getCacheTtlMinutes() {
        return cacheTtlMinutes;
    }

    public double getMaxWindSpeed() {
        return maxWindSpeed;
    }

    public double getMinTemperature() {
        return minTemperature;
    }

    public double getMaxTemperature() {
        return maxTemperature;
    }
}
