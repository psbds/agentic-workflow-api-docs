package com.hotel.reservations.domain.dto;

public record HotelDto(
        Long id,
        String name,
        String address,
        String city,
        String country,
        int starRating,
        String description,
        Double latitude,
        Double longitude,
        String phoneNumber,
        String email,
        int totalRooms
) {
}
