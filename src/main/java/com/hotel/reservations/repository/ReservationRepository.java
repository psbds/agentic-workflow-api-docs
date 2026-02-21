package com.hotel.reservations.repository;

import com.hotel.reservations.domain.entities.Reservation;
import com.hotel.reservations.domain.enums.ReservationStatus;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class ReservationRepository implements PanacheRepository<Reservation> {

    public Optional<Reservation> findByConfirmationCode(String code) {
        return find("confirmationCode", code).firstResultOptional();
    }

    public List<Reservation> findByGuestId(Long guestId) {
        return list("guest.id", guestId);
    }

    public List<Reservation> findByRoomId(Long roomId) {
        return list("room.id", roomId);
    }

    public List<Reservation> findByStatus(ReservationStatus status) {
        return list("status", status);
    }

    public List<Reservation> findActiveByRoomId(Long roomId) {
        return list("room.id = ?1 and status in ('PENDING', 'CONFIRMED', 'CHECKED_IN')", roomId);
    }

    public List<Reservation> findOverlapping(Long roomId, LocalDate checkIn, LocalDate checkOut) {
        return list("room.id = ?1 and status not in ('CANCELLED', 'EXPIRED') "
                + "and checkInDate < ?3 and checkOutDate > ?2", roomId, checkIn, checkOut);
    }

    public List<Reservation> findByCheckInDate(LocalDate date) {
        return list("checkInDate", date);
    }

    public List<Reservation> findExpiredPendingReservations(LocalDate beforeDate) {
        return list("status = 'PENDING' and checkInDate < ?1", beforeDate);
    }

    public List<Reservation> findByGuestIdAndStatus(Long guestId, ReservationStatus status) {
        return list("guest.id = ?1 and status = ?2", guestId, status);
    }
}
