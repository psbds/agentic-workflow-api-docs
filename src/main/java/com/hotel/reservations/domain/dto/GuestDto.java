package com.hotel.reservations.domain.dto;

public record GuestDto(
        Long id,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        String documentType,
        String documentNumber,
        String nationality
) {
}
