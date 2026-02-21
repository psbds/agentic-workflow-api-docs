package com.hotel.reservations.resources;

import com.hotel.reservations.domain.dto.ApiResponse;
import com.hotel.reservations.domain.dto.CreateReservationRequest;
import com.hotel.reservations.domain.dto.ReservationDto;
import com.hotel.reservations.domain.dto.UpdateReservationRequest;
import com.hotel.reservations.mappers.ReservationMapper;
import com.hotel.reservations.services.ReservationService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import java.util.List;

@Path("/api/v1/reservations")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Reservations", description = "Reservation management operations")
public class ReservationResource {

    private static final Logger LOG = Logger.getLogger(ReservationResource.class);

    @Inject
    ReservationService reservationService;

    @Inject
    ReservationMapper reservationMapper;

    @POST
    public Response createReservation(@Valid CreateReservationRequest request) {
        LOG.info("Creating reservation");
        var reservation = reservationService.createReservation(request);
        return Response.status(Response.Status.CREATED)
                .entity(ApiResponse.success(reservationMapper.toDto(reservation), "Reservation created successfully"))
                .build();
    }

    @GET
    @Path("/{id}")
    public ApiResponse<ReservationDto> getReservation(@PathParam("id") Long id) {
        LOG.infof("Getting reservation with id: %d", id);
        var reservation = reservationService.findById(id);
        return ApiResponse.success(reservationMapper.toDto(reservation), "Reservation retrieved successfully");
    }

    @GET
    @Path("/confirmation/{code}")
    public ApiResponse<ReservationDto> getByConfirmationCode(@PathParam("code") String code) {
        LOG.infof("Getting reservation by confirmation code: %s", code);
        var reservation = reservationService.findByConfirmationCode(code);
        return ApiResponse.success(reservationMapper.toDto(reservation), "Reservation retrieved successfully");
    }

    @GET
    @Path("/guest/{guestId}")
    public ApiResponse<List<ReservationDto>> getByGuestId(@PathParam("guestId") Long guestId) {
        LOG.infof("Getting reservations for guest: %d", guestId);
        var reservations = reservationService.findByGuestId(guestId);
        return ApiResponse.success(reservationMapper.toDtoList(reservations), "Reservations retrieved successfully");
    }

    @PUT
    @Path("/{id}")
    public ApiResponse<ReservationDto> updateReservation(
            @PathParam("id") Long id,
            @Valid UpdateReservationRequest request) {
        LOG.infof("Updating reservation with id: %d", id);
        var updated = reservationService.updateReservation(id, request);
        return ApiResponse.success(reservationMapper.toDto(updated), "Reservation updated successfully");
    }

    @POST
    @Path("/{id}/confirm")
    public ApiResponse<ReservationDto> confirmReservation(@PathParam("id") Long id) {
        LOG.infof("Confirming reservation: %d", id);
        var reservation = reservationService.confirmReservation(id);
        return ApiResponse.success(reservationMapper.toDto(reservation), "Reservation confirmed successfully");
    }

    @POST
    @Path("/{id}/cancel")
    public ApiResponse<ReservationDto> cancelReservation(@PathParam("id") Long id) {
        LOG.infof("Cancelling reservation: %d", id);
        var reservation = reservationService.cancelReservation(id);
        return ApiResponse.success(reservationMapper.toDto(reservation), "Reservation cancelled successfully");
    }

    @POST
    @Path("/{id}/checkin")
    public ApiResponse<ReservationDto> checkIn(@PathParam("id") Long id) {
        LOG.infof("Checking in reservation: %d", id);
        var reservation = reservationService.checkIn(id);
        return ApiResponse.success(reservationMapper.toDto(reservation), "Check-in successful");
    }

    @POST
    @Path("/{id}/checkout")
    public ApiResponse<ReservationDto> checkOut(@PathParam("id") Long id) {
        LOG.infof("Checking out reservation: %d", id);
        var reservation = reservationService.checkOut(id);
        return ApiResponse.success(reservationMapper.toDto(reservation), "Check-out successful");
    }
}
