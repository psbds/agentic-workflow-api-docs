package com.hotel.reservations.backends;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient
@Path("/v1")
public interface WeatherApiClient {

    @GET
    @Path("/forecast")
    WeatherApiResponse getForecast(
        @QueryParam("latitude") double latitude,
        @QueryParam("longitude") double longitude,
        @QueryParam("daily") String dailyParams,
        @QueryParam("timezone") String timezone,
        @QueryParam("start_date") String startDate,
        @QueryParam("end_date") String endDate
    );
}
