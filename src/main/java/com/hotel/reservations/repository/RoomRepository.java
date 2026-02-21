package com.hotel.reservations.repository;

import com.hotel.reservations.domain.entities.Room;
import com.hotel.reservations.domain.enums.RoomType;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class RoomRepository implements PanacheRepository<Room> {

    public List<Room> findByHotelId(Long hotelId) {
        return list("hotel.id", hotelId);
    }

    public List<Room> findAvailableByHotelId(Long hotelId) {
        return list("hotel.id = ?1 and isAvailable = true", hotelId);
    }

    public List<Room> findByRoomType(RoomType type) {
        return list("roomType", type);
    }

    public List<Room> findAvailableRooms(Long hotelId, LocalDate checkIn, LocalDate checkOut) {
        return list("hotel.id = ?1 and id not in ("
                + "select r.room.id from Reservation r "
                + "where r.room.hotel.id = ?1 "
                + "and r.status not in ('CANCELLED', 'EXPIRED') "
                + "and r.checkInDate < ?3 and r.checkOutDate > ?2"
                + ")", hotelId, checkIn, checkOut);
    }

    public Optional<Room> findByHotelIdAndRoomNumber(Long hotelId, String roomNumber) {
        return find("hotel.id = ?1 and roomNumber = ?2", hotelId, roomNumber).firstResultOptional();
    }
}
