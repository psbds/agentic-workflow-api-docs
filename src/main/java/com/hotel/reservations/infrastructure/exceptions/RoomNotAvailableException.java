package com.hotel.reservations.infrastructure.exceptions;

public class RoomNotAvailableException extends HotelException {

    public RoomNotAvailableException(String roomId) {
        super("ROOM_NOT_AVAILABLE", "Room with id '" + roomId + "' is not available");
    }
}
