package com.hotel.reservations.resources;

import com.hotel.reservations.domain.dto.ApiResponse;
import com.hotel.reservations.domain.dto.HotelDto;
import com.hotel.reservations.domain.entities.Hotel;
import com.hotel.reservations.mappers.HotelMapper;
import com.hotel.reservations.services.HotelService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import java.util.List;

@Path("/api/v1/hotels")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Hotels", description = "Hotel management operations")
public class HotelResource {

    private static final Logger LOG = Logger.getLogger(HotelResource.class);

    @Inject
    HotelService hotelService;

    @Inject
    HotelMapper hotelMapper;

    @GET
    public ApiResponse<List<HotelDto>> listHotels(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        LOG.infof("Listing hotels - page: %d, size: %d", page, size);
        var hotels = hotelService.findAll(page, size);
        return ApiResponse.success(hotelMapper.toDtoList(hotels), "Hotels retrieved successfully");
    }

    @GET
    @Path("/{id}")
    public ApiResponse<HotelDto> getHotel(@PathParam("id") Long id) {
        LOG.infof("Getting hotel with id: %d", id);
        var hotel = hotelService.findById(id);
        return ApiResponse.success(hotelMapper.toDto(hotel), "Hotel retrieved successfully");
    }

    @GET
    @Path("/city/{city}")
    public ApiResponse<List<HotelDto>> getByCity(@PathParam("city") String city) {
        LOG.infof("Getting hotels in city: %s", city);
        var hotels = hotelService.findByCity(city);
        return ApiResponse.success(hotelMapper.toDtoList(hotels), "Hotels retrieved successfully");
    }

    @GET
    @Path("/search")
    public ApiResponse<List<HotelDto>> searchByName(@QueryParam("name") String name) {
        LOG.infof("Searching hotels by name: %s", name);
        var hotels = hotelService.searchByName(name);
        return ApiResponse.success(hotelMapper.toDtoList(hotels), "Hotels retrieved successfully");
    }

    @POST
    public Response createHotel(@Valid Hotel hotel) {
        LOG.infof("Creating hotel: %s", hotel.getName());
        var created = hotelService.create(hotel);
        return Response.status(Response.Status.CREATED)
                .entity(ApiResponse.success(hotelMapper.toDto(created), "Hotel created successfully"))
                .build();
    }

    @PUT
    @Path("/{id}")
    public ApiResponse<HotelDto> updateHotel(@PathParam("id") Long id, Hotel hotel) {
        LOG.infof("Updating hotel with id: %d", id);
        var updated = hotelService.update(id, hotel);
        return ApiResponse.success(hotelMapper.toDto(updated), "Hotel updated successfully");
    }

    @DELETE
    @Path("/{id}")
    public Response deleteHotel(@PathParam("id") Long id) {
        LOG.infof("Deleting hotel with id: %d", id);
        hotelService.delete(id);
        return Response.noContent().build();
    }
}
