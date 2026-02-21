package com.hotel.reservations.services;

import com.hotel.reservations.domain.entities.Guest;
import com.hotel.reservations.infrastructure.config.CacheConfig;
import com.hotel.reservations.repository.GuestRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class GuestService {

    private static final Logger LOG = Logger.getLogger(GuestService.class);

    @Inject
    GuestRepository guestRepository;

    @Inject
    CacheConfig cacheConfig;

    public Guest findById(Long id) {
        Guest guest = guestRepository.findById(id);
        if (guest == null) {
            throw new NotFoundException("Guest not found with id: " + id);
        }
        return guest;
    }

    public Optional<Guest> findByEmail(String email) {
        return guestRepository.findByEmail(email);
    }

    @Transactional
    public Guest create(Guest guest) {
        guestRepository.persist(guest);
        LOG.infof("Guest created: id=%d, email=%s", guest.id, guest.email);
        return guest;
    }

    @Transactional
    public Guest update(Long id, Guest guest) {
        Guest existing = guestRepository.findById(id);
        if (existing == null) {
            throw new NotFoundException("Guest not found with id: " + id);
        }

        existing.firstName = guest.firstName;
        existing.lastName = guest.lastName;
        existing.email = guest.email;
        existing.phoneNumber = guest.phoneNumber;
        existing.documentType = guest.documentType;
        existing.documentNumber = guest.documentNumber;
        existing.nationality = guest.nationality;

        guestRepository.persist(existing);
        LOG.infof("Guest updated: id=%d", id);
        return existing;
    }

    public List<Guest> searchByName(String name) {
        return guestRepository.searchByName(name);
    }
}
