package com.hotel.reservations.repository;

import com.hotel.reservations.domain.entities.Hotel;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class HotelRepository implements PanacheRepository<Hotel> {

    public List<Hotel> findByCity(String city) {
        return list("city", city);
    }

    public List<Hotel> findByCountry(String country) {
        return list("country", country);
    }

    public List<Hotel> findByNameContaining(String name) {
        return list("lower(name) like lower(?1)", "%" + name + "%");
    }

    public List<Hotel> findByStarRating(int rating) {
        return list("starRating", rating);
    }

    public List<Hotel> findAllPaged(int page, int size) {
        return findAll().page(page, size).list();
    }
}
