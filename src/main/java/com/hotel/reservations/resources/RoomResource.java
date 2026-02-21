package com.hotel.reservations.resources;

import com.hotel.reservations.domain.dto.ApiResponse;
import com.hotel.reservations.domain.dto.RoomDto;
import com.hotel.reservations.domain.entities.Room;
import com.hotel.reservations.mappers.RoomMapper;
import com.hotel.reservations.services.RoomService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import java.time.LocalDate;
import java.util.List;

@Path("/api/v1/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Rooms", description = "Room management operations")
public class RoomResource {

    private static final Logger LOG = Logger.getLogger(RoomResource.class);

    @Inject
    RoomService roomService;

    @Inject
    RoomMapper roomMapper;

    @GET
    @Path("/hotel/{hotelId}")
    public ApiResponse<List<RoomDto>> getRoomsByHotel(@PathParam("hotelId") Long hotelId) {
        LOG.infof("Getting rooms for hotel: %d", hotelId);
        var rooms = roomService.findByHotelId(hotelId);
        return ApiResponse.success(roomMapper.toDtoList(rooms), "Rooms retrieved successfully");
    }

    @GET
    @Path("/hotel/{hotelId}/available")
    public ApiResponse<List<RoomDto>> getAvailableRooms(
            @PathParam("hotelId") Long hotelId,
            @QueryParam("checkIn") String checkIn,
            @QueryParam("checkOut") String checkOut) {
        LOG.infof("Getting available rooms for hotel: %d, checkIn: %s, checkOut: %s", hotelId, checkIn, checkOut);
        var checkInDate = LocalDate.parse(checkIn);
        var checkOutDate = LocalDate.parse(checkOut);
        var rooms = roomService.findAvailableRooms(hotelId, checkInDate, checkOutDate);
        return ApiResponse.success(roomMapper.toDtoList(rooms), "Available rooms retrieved successfully");
    }

    @GET
    @Path("/{id}")
    public ApiResponse<RoomDto> getRoom(@PathParam("id") Long id) {
        LOG.infof("Getting room with id: %d", id);
        var room = roomService.findById(id);
        return ApiResponse.success(roomMapper.toDto(room), "Room retrieved successfully");
    }

    @POST
    public Response createRoom(@Valid Room room) {
        LOG.info("Creating room");
        var created = roomService.create(room);
        return Response.status(Response.Status.CREATED)
                .entity(ApiResponse.success(roomMapper.toDto(created), "Room created successfully"))
                .build();
    }

    @PUT
    @Path("/{id}")
    public ApiResponse<RoomDto> updateRoom(@PathParam("id") Long id, Room room) {
        LOG.infof("Updating room with id: %d", id);
        var updated = roomService.update(id, room);
        return ApiResponse.success(roomMapper.toDto(updated), "Room updated successfully");
    }

    @PATCH
    @Path("/{id}/availability")
    public ApiResponse<RoomDto> updateAvailability(
            @PathParam("id") Long id,
            @QueryParam("available") boolean available) {
        LOG.infof("Updating availability for room: %d to %s", id, available);
        var updated = roomService.updateAvailability(id, available);
        return ApiResponse.success(roomMapper.toDto(updated), "Room availability updated successfully");
    }
}
