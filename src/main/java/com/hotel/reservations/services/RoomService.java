package com.hotel.reservations.services;

import com.hotel.reservations.domain.entities.Room;
import com.hotel.reservations.infrastructure.config.CacheConfig;
import com.hotel.reservations.repository.HotelRepository;
import com.hotel.reservations.repository.RoomRepository;
import com.hotel.reservations.utils.Constants;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import org.jboss.logging.Logger;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class RoomService {

    private static final Logger LOG = Logger.getLogger(RoomService.class);

    @Inject
    RoomRepository roomRepository;

    @Inject
    HotelRepository hotelRepository;

    @Inject
    CacheConfig cacheConfig;

    public List<Room> findByHotelId(Long hotelId) {
        return roomRepository.findByHotelId(hotelId);
    }

    public List<Room> findAvailableRooms(Long hotelId, LocalDate checkIn, LocalDate checkOut) {
        String cacheKey = Constants.CACHE_PREFIX_AVAILABILITY + hotelId + ":" + checkIn + ":" + checkOut;
        Optional<Room[]> cached = cacheConfig.getObject(cacheKey, Room[].class);
        if (cached.isPresent()) {
            return List.of(cached.get());
        }

        List<Room> rooms = roomRepository.findAvailableRooms(hotelId, checkIn, checkOut);
        cacheConfig.putObject(cacheKey, rooms.toArray(new Room[0]), 300);
        return rooms;
    }

    public Room findById(Long id) {
        String cacheKey = Constants.CACHE_PREFIX_ROOM + id;
        Optional<Room> cached = cacheConfig.getObject(cacheKey, Room.class);
        if (cached.isPresent()) {
            return cached.get();
        }

        Room room = roomRepository.findById(id);
        if (room == null) {
            throw new NotFoundException("Room not found with id: " + id);
        }

        cacheConfig.putObject(cacheKey, room, 600);
        return room;
    }

    @Transactional
    public Room create(Room room) {
        if (room.hotel == null || hotelRepository.findById(room.hotel.id) == null) {
            throw new NotFoundException("Hotel not found for the specified room");
        }
        roomRepository.persist(room);
        LOG.infof("Room created: id=%d, roomNumber=%s", room.id, room.roomNumber);
        return room;
    }

    @Transactional
    public Room update(Long id, Room room) {
        Room existing = roomRepository.findById(id);
        if (existing == null) {
            throw new NotFoundException("Room not found with id: " + id);
        }

        existing.roomNumber = room.roomNumber;
        existing.roomType = room.roomType;
        existing.pricePerNight = room.pricePerNight;
        existing.maxOccupancy = room.maxOccupancy;
        existing.description = room.description;
        existing.isAvailable = room.isAvailable;
        existing.floorNumber = room.floorNumber;

        roomRepository.persist(existing);
        cacheConfig.delete(Constants.CACHE_PREFIX_ROOM + id);
        LOG.infof("Room updated: id=%d", id);
        return existing;
    }

    @Transactional
    public Room updateAvailability(Long id, boolean available) {
        Room room = roomRepository.findById(id);
        if (room == null) {
            throw new NotFoundException("Room not found with id: " + id);
        }

        room.isAvailable = available;
        roomRepository.persist(room);
        cacheConfig.delete(Constants.CACHE_PREFIX_ROOM + id);
        LOG.infof("Room availability updated: id=%d, available=%s", id, available);
        return room;
    }
}
