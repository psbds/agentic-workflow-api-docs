package com.hotel.reservations.resources;

import com.hotel.reservations.domain.dto.ApiResponse;
import com.hotel.reservations.domain.dto.WeatherDto;
import com.hotel.reservations.services.WeatherService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import java.time.LocalDate;

@Path("/api/v1/weather")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Weather", description = "Weather forecast operations")
public class WeatherResource {

    private static final Logger LOG = Logger.getLogger(WeatherResource.class);

    @Inject
    WeatherService weatherService;

    @GET
    @Path("/check")
    public ApiResponse<WeatherDto> checkWeather(
            @QueryParam("latitude") double latitude,
            @QueryParam("longitude") double longitude,
            @QueryParam("date") String date) {
        LOG.infof("Checking weather for lat: %f, lon: %f, date: %s", latitude, longitude, date);
        var localDate = LocalDate.parse(date);
        var weather = weatherService.checkWeather(latitude, longitude, localDate);
        return ApiResponse.success(weather, "Weather retrieved successfully");
    }
}
