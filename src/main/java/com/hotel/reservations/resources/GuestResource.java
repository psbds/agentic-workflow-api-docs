package com.hotel.reservations.resources;

import com.hotel.reservations.domain.dto.ApiResponse;
import com.hotel.reservations.domain.dto.GuestDto;
import com.hotel.reservations.domain.entities.Guest;
import com.hotel.reservations.mappers.GuestMapper;
import com.hotel.reservations.services.GuestService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import java.util.List;

@Path("/api/v1/guests")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Guests", description = "Guest management operations")
public class GuestResource {

    private static final Logger LOG = Logger.getLogger(GuestResource.class);

    @Inject
    GuestService guestService;

    @Inject
    GuestMapper guestMapper;

    @GET
    @Path("/{id}")
    public ApiResponse<GuestDto> getGuest(@PathParam("id") Long id) {
        LOG.infof("Getting guest with id: %d", id);
        var guest = guestService.findById(id);
        return ApiResponse.success(guestMapper.toDto(guest), "Guest retrieved successfully");
    }

    @GET
    @Path("/email/{email}")
    public ApiResponse<GuestDto> getByEmail(@PathParam("email") String email) {
        LOG.infof("Getting guest by email: %s", email);
        var guest = guestService.findByEmail(email);
        return ApiResponse.success(guestMapper.toDto(guest), "Guest retrieved successfully");
    }

    @GET
    @Path("/search")
    public ApiResponse<List<GuestDto>> searchByName(@QueryParam("name") String name) {
        LOG.infof("Searching guests by name: %s", name);
        var guests = guestService.searchByName(name);
        return ApiResponse.success(guestMapper.toDtoList(guests), "Guests retrieved successfully");
    }

    @POST
    public Response createGuest(@Valid Guest guest) {
        LOG.infof("Creating guest: %s", guest.getEmail());
        var created = guestService.create(guest);
        return Response.status(Response.Status.CREATED)
                .entity(ApiResponse.success(guestMapper.toDto(created), "Guest created successfully"))
                .build();
    }

    @PUT
    @Path("/{id}")
    public ApiResponse<GuestDto> updateGuest(@PathParam("id") Long id, Guest guest) {
        LOG.infof("Updating guest with id: %d", id);
        var updated = guestService.update(id, guest);
        return ApiResponse.success(guestMapper.toDto(updated), "Guest updated successfully");
    }
}
