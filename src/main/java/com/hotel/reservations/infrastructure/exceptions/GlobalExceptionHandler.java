package com.hotel.reservations.infrastructure.exceptions;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.time.Instant;
import java.util.Map;

@Provider
public class GlobalExceptionHandler implements ExceptionMapper<HotelException> {

    @Override
    public Response toResponse(HotelException exception) {
        int status = mapToHttpStatus(exception);

        Map<String, Object> body = Map.of(
                "success", false,
                "message", exception.getMessage(),
                "errorCode", exception.getErrorCode(),
                "timestamp", Instant.now().toString()
        );

        return Response.status(status).entity(body).build();
    }

    private int mapToHttpStatus(HotelException exception) {
        if (exception instanceof ReservationNotFoundException) {
            return Response.Status.NOT_FOUND.getStatusCode();
        } else if (exception instanceof RoomNotAvailableException) {
            return Response.Status.CONFLICT.getStatusCode();
        } else if (exception instanceof WeatherCheckFailedException) {
            return Response.Status.SERVICE_UNAVAILABLE.getStatusCode();
        } else if (exception instanceof InvalidDateRangeException) {
            return Response.Status.BAD_REQUEST.getStatusCode();
        }
        return Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
    }
}
