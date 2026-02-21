package com.hotel.reservations.services;

import com.hotel.reservations.backends.WeatherApiClient;
import com.hotel.reservations.backends.WeatherApiResponse;
import com.hotel.reservations.backends.WeatherCodeMapper;
import com.hotel.reservations.domain.dto.WeatherDto;
import com.hotel.reservations.infrastructure.config.CacheConfig;
import com.hotel.reservations.infrastructure.config.HotelConfig;
import com.hotel.reservations.infrastructure.exceptions.WeatherCheckFailedException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.time.LocalDate;
import java.util.Optional;

@ApplicationScoped
public class WeatherService {

    private static final Logger LOG = Logger.getLogger(WeatherService.class);
    private static final String DAILY_PARAMS = "temperature_2m_max,temperature_2m_min,wind_speed_10m_max,weather_code";

    @Inject
    @RestClient
    WeatherApiClient weatherApiClient;

    @Inject
    CacheConfig cacheConfig;

    @Inject
    HotelConfig hotelConfig;

    public WeatherDto checkWeather(double latitude, double longitude, LocalDate checkInDate) {
        String cacheKey = "weather:" + latitude + ":" + longitude + ":" + checkInDate;

        Optional<WeatherDto> cached = cacheConfig.getObject(cacheKey, WeatherDto.class);
        if (cached.isPresent()) {
            LOG.debugf("Weather cache hit for key: %s", cacheKey);
            return cached.get();
        }

        try {
            LOG.infof("Fetching weather forecast for lat=%f, lon=%f, date=%s", latitude, longitude, checkInDate);

            WeatherApiResponse response = weatherApiClient.getForecast(
                    latitude, longitude, DAILY_PARAMS, "auto",
                    checkInDate.toString(), checkInDate.toString());

            WeatherApiResponse.Daily daily = response.getDaily();

            double tempMax = daily.getTemperatureMax().get(0);
            double tempMin = daily.getTemperatureMin().get(0);
            double avgTemp = (tempMax + tempMin) / 2.0;
            double windSpeed = daily.getWindSpeedMax().get(0);
            int weatherCode = daily.getWeatherCode().get(0);

            String description = WeatherCodeMapper.getDescription(weatherCode);
            boolean isSevere = WeatherCodeMapper.isSevereWeather(weatherCode);

            boolean isSuitableForTravel = !isSevere
                    && windSpeed < hotelConfig.getMaxWindSpeed()
                    && avgTemp >= hotelConfig.getMinTemperature()
                    && avgTemp <= hotelConfig.getMaxTemperature();

            WeatherDto weatherDto = new WeatherDto(
                    avgTemp, windSpeed, description, null, checkInDate, isSuitableForTravel);

            long ttlSeconds = (long) hotelConfig.getCacheTtlMinutes() * 60;
            cacheConfig.putObject(cacheKey, weatherDto, ttlSeconds);

            LOG.infof("Weather check complete: temp=%.1f, wind=%.1f, suitable=%s", avgTemp, windSpeed, isSuitableForTravel);
            return weatherDto;

        } catch (Exception e) {
            LOG.errorf(e, "Failed to check weather for lat=%f, lon=%f, date=%s", latitude, longitude, checkInDate);
            throw new WeatherCheckFailedException("Unable to retrieve weather forecast: " + e.getMessage());
        }
    }
}
