package com.hotel.reservations.services;

import com.hotel.reservations.domain.entities.Hotel;
import com.hotel.reservations.infrastructure.config.CacheConfig;
import com.hotel.reservations.infrastructure.config.HotelConfig;
import com.hotel.reservations.repository.HotelRepository;
import com.hotel.reservations.utils.Constants;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class HotelService {

    private static final Logger LOG = Logger.getLogger(HotelService.class);

    @Inject
    HotelRepository hotelRepository;

    @Inject
    CacheConfig cacheConfig;

    @Inject
    HotelConfig hotelConfig;

    public List<Hotel> findAll(int page, int size) {
        String cacheKey = Constants.CACHE_PREFIX_HOTEL + "all:" + page + ":" + size;
        Optional<Hotel[]> cached = cacheConfig.getObject(cacheKey, Hotel[].class);
        if (cached.isPresent()) {
            LOG.debugf("Hotel list cache hit for page=%d, size=%d", page, size);
            return List.of(cached.get());
        }

        List<Hotel> hotels = hotelRepository.findAllPaged(page, size);
        long ttl = (long) hotelConfig.getCacheTtlMinutes() * 60;
        cacheConfig.putObject(cacheKey, hotels.toArray(new Hotel[0]), ttl);
        return hotels;
    }

    public Hotel findById(Long id) {
        String cacheKey = Constants.CACHE_PREFIX_HOTEL + id;
        Optional<Hotel> cached = cacheConfig.getObject(cacheKey, Hotel.class);
        if (cached.isPresent()) {
            return cached.get();
        }

        Hotel hotel = hotelRepository.findById(id);
        if (hotel == null) {
            throw new NotFoundException("Hotel not found with id: " + id);
        }

        long ttl = (long) hotelConfig.getCacheTtlMinutes() * 60;
        cacheConfig.putObject(cacheKey, hotel, ttl);
        return hotel;
    }

    public List<Hotel> findByCity(String city) {
        return hotelRepository.findByCity(city);
    }

    public List<Hotel> searchByName(String name) {
        return hotelRepository.findByNameContaining(name);
    }

    @Transactional
    public Hotel create(Hotel hotel) {
        hotelRepository.persist(hotel);
        cacheConfig.delete(Constants.CACHE_PREFIX_HOTEL + "all:0:20");
        LOG.infof("Hotel created: id=%d, name=%s", hotel.id, hotel.name);
        return hotel;
    }

    @Transactional
    public Hotel update(Long id, Hotel hotel) {
        Hotel existing = hotelRepository.findById(id);
        if (existing == null) {
            throw new NotFoundException("Hotel not found with id: " + id);
        }

        existing.name = hotel.name;
        existing.address = hotel.address;
        existing.city = hotel.city;
        existing.country = hotel.country;
        existing.starRating = hotel.starRating;
        existing.description = hotel.description;
        existing.latitude = hotel.latitude;
        existing.longitude = hotel.longitude;
        existing.phoneNumber = hotel.phoneNumber;
        existing.email = hotel.email;

        hotelRepository.persist(existing);
        cacheConfig.delete(Constants.CACHE_PREFIX_HOTEL + id);
        LOG.infof("Hotel updated: id=%d", id);
        return existing;
    }

    @Transactional
    public void delete(Long id) {
        Hotel hotel = hotelRepository.findById(id);
        if (hotel == null) {
            throw new NotFoundException("Hotel not found with id: " + id);
        }
        hotelRepository.delete(hotel);
        cacheConfig.delete(Constants.CACHE_PREFIX_HOTEL + id);
        LOG.infof("Hotel deleted: id=%d", id);
    }
}
