package com.hotel.reservations.repository;

import com.hotel.reservations.domain.entities.Guest;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class GuestRepository implements PanacheRepository<Guest> {

    public Optional<Guest> findByEmail(String email) {
        return find("email", email).firstResultOptional();
    }

    public List<Guest> findByLastName(String lastName) {
        return list("lastName", lastName);
    }

    public Optional<Guest> findByDocumentNumber(String documentNumber) {
        return find("documentNumber", documentNumber).firstResultOptional();
    }

    public List<Guest> searchByName(String name) {
        return list("lower(firstName) like lower(?1) or lower(lastName) like lower(?1)", "%" + name + "%");
    }
}
