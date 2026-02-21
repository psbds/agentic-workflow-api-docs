package com.hotel.reservations.domain.dto;

import com.hotel.reservations.domain.enums.RoomType;

import java.math.BigDecimal;

public record RoomDto(
        Long id,
        String roomNumber,
        RoomType roomType,
        BigDecimal pricePerNight,
        int maxOccupancy,
        String description,
        boolean isAvailable,
        int floorNumber,
        Long hotelId,
        String hotelName
) {
}
