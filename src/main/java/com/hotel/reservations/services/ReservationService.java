package com.hotel.reservations.services;

import com.hotel.reservations.domain.dto.CreateReservationRequest;
import com.hotel.reservations.domain.dto.UpdateReservationRequest;
import com.hotel.reservations.domain.dto.WeatherDto;
import com.hotel.reservations.domain.entities.Guest;
import com.hotel.reservations.domain.entities.Reservation;
import com.hotel.reservations.domain.entities.Room;
import com.hotel.reservations.domain.enums.PaymentStatus;
import com.hotel.reservations.domain.enums.ReservationStatus;
import com.hotel.reservations.infrastructure.config.CacheConfig;
import com.hotel.reservations.infrastructure.config.HotelConfig;
import com.hotel.reservations.infrastructure.exceptions.InvalidDateRangeException;
import com.hotel.reservations.infrastructure.exceptions.ReservationNotFoundException;
import com.hotel.reservations.infrastructure.exceptions.RoomNotAvailableException;
import com.hotel.reservations.infrastructure.exceptions.WeatherCheckFailedException;
import com.hotel.reservations.messaging.ReservationEvent;
import com.hotel.reservations.messaging.ReservationEventPublisher;
import com.hotel.reservations.repository.GuestRepository;
import com.hotel.reservations.repository.ReservationRepository;
import com.hotel.reservations.repository.RoomRepository;
import com.hotel.reservations.utils.ConfirmationCodeGenerator;
import com.hotel.reservations.utils.Constants;
import com.hotel.reservations.utils.DateUtils;
import com.hotel.reservations.utils.PriceCalculator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class ReservationService {

    private static final Logger LOG = Logger.getLogger(ReservationService.class);

    @Inject
    ReservationRepository reservationRepository;

    @Inject
    RoomRepository roomRepository;

    @Inject
    GuestRepository guestRepository;

    @Inject
    WeatherService weatherService;

    @Inject
    ReservationEventPublisher eventPublisher;

    @Inject
    CacheConfig cacheConfig;

    @Inject
    HotelConfig hotelConfig;

    @Transactional
    public Reservation createReservation(CreateReservationRequest request) {
        if (!DateUtils.isDateRangeValid(request.checkInDate(), request.checkOutDate())) {
            throw new InvalidDateRangeException("Check-out date must be after check-in date");
        }

        if (!DateUtils.isWithinMaxDays(request.checkInDate(), request.checkOutDate(), hotelConfig.getMaxReservationDays())) {
            throw new InvalidDateRangeException(
                    "Reservation exceeds maximum allowed duration of " + hotelConfig.getMaxReservationDays() + " days");
        }

        Guest guest = guestRepository.findById(request.guestId());
        if (guest == null) {
            throw new NotFoundException("Guest not found with id: " + request.guestId());
        }

        Room room = roomRepository.findById(request.roomId());
        if (room == null) {
            throw new NotFoundException("Room not found with id: " + request.roomId());
        }

        List<Reservation> overlapping = reservationRepository.findOverlapping(
                room.id, request.checkInDate(), request.checkOutDate());
        if (!overlapping.isEmpty()) {
            throw new RoomNotAvailableException(room.id.toString());
        }

        WeatherDto weather = weatherService.checkWeather(
                room.hotel.latitude, room.hotel.longitude, request.checkInDate());
        if (!weather.isSuitableForTravel()) {
            throw new WeatherCheckFailedException(
                    "Weather is not suitable for travel: " + weather.weatherDescription()
                            + " (temp: " + String.format("%.1f", weather.temperature())
                            + "°C, wind: " + String.format("%.1f", weather.windSpeed()) + " km/h)");
        }

        BigDecimal totalPrice = PriceCalculator.calculateTotalPrice(
                room.pricePerNight, request.checkInDate(), request.checkOutDate());

        Reservation reservation = new Reservation();
        reservation.confirmationCode = ConfirmationCodeGenerator.generate();
        reservation.checkInDate = request.checkInDate();
        reservation.checkOutDate = request.checkOutDate();
        reservation.numberOfGuests = request.numberOfGuests();
        reservation.totalPrice = totalPrice;
        reservation.status = ReservationStatus.PENDING;
        reservation.paymentStatus = PaymentStatus.PENDING;
        reservation.specialRequests = request.specialRequests();
        reservation.weatherChecked = true;
        reservation.weatherSummary = weather.weatherDescription()
                + " | Temp: " + String.format("%.1f", weather.temperature())
                + "°C | Wind: " + String.format("%.1f", weather.windSpeed()) + " km/h";
        reservation.guest = guest;
        reservation.room = room;

        reservationRepository.persist(reservation);
        LOG.infof("Reservation created: id=%d, code=%s", reservation.id, reservation.confirmationCode);

        publishEvent(ReservationEvent.EventType.CREATED, reservation);

        return reservation;
    }

    @Transactional
    public Reservation confirmReservation(Long id) {
        Reservation reservation = findById(id);
        reservation.status = ReservationStatus.CONFIRMED;
        reservationRepository.persist(reservation);
        cacheConfig.delete(Constants.CACHE_PREFIX_RESERVATION + id);
        LOG.infof("Reservation confirmed: id=%d, code=%s", id, reservation.confirmationCode);

        publishEvent(ReservationEvent.EventType.CONFIRMED, reservation);
        return reservation;
    }

    @Transactional
    public Reservation cancelReservation(Long id) {
        Reservation reservation = findById(id);
        reservation.status = ReservationStatus.CANCELLED;

        BigDecimal refund = PriceCalculator.calculateRefundAmount(
                reservation.totalPrice, reservation.checkInDate, hotelConfig.getCancellationHoursBefore());
        reservation.totalPrice = refund;

        reservationRepository.persist(reservation);
        cacheConfig.delete(Constants.CACHE_PREFIX_RESERVATION + id);
        LOG.infof("Reservation cancelled: id=%d, code=%s, refund=%s", id, reservation.confirmationCode, refund);

        publishEvent(ReservationEvent.EventType.CANCELLED, reservation);
        return reservation;
    }

    @Transactional
    public Reservation checkIn(Long id) {
        Reservation reservation = findById(id);
        reservation.status = ReservationStatus.CHECKED_IN;
        reservationRepository.persist(reservation);
        cacheConfig.delete(Constants.CACHE_PREFIX_RESERVATION + id);
        LOG.infof("Guest checked in: reservationId=%d, code=%s", id, reservation.confirmationCode);

        publishEvent(ReservationEvent.EventType.CHECKED_IN, reservation);
        return reservation;
    }

    @Transactional
    public Reservation checkOut(Long id) {
        Reservation reservation = findById(id);
        reservation.status = ReservationStatus.CHECKED_OUT;
        reservationRepository.persist(reservation);
        cacheConfig.delete(Constants.CACHE_PREFIX_RESERVATION + id);
        LOG.infof("Guest checked out: reservationId=%d, code=%s", id, reservation.confirmationCode);

        publishEvent(ReservationEvent.EventType.CHECKED_OUT, reservation);
        return reservation;
    }

    public Reservation findById(Long id) {
        String cacheKey = Constants.CACHE_PREFIX_RESERVATION + id;
        var cached = cacheConfig.getObject(cacheKey, Reservation.class);
        if (cached.isPresent()) {
            return cached.get();
        }

        Reservation reservation = reservationRepository.findById(id);
        if (reservation == null) {
            throw new ReservationNotFoundException(id.toString());
        }

        long ttl = (long) hotelConfig.getCacheTtlMinutes() * 60;
        cacheConfig.putObject(cacheKey, reservation, ttl);
        return reservation;
    }

    public Reservation findByConfirmationCode(String code) {
        return reservationRepository.findByConfirmationCode(code)
                .orElseThrow(() -> new ReservationNotFoundException(code));
    }

    public List<Reservation> findByGuestId(Long guestId) {
        return reservationRepository.findByGuestId(guestId);
    }

    @Transactional
    public Reservation updateReservation(Long id, UpdateReservationRequest request) {
        Reservation reservation = findById(id);

        if (request.checkInDate() != null && request.checkOutDate() != null) {
            if (!DateUtils.isDateRangeValid(request.checkInDate(), request.checkOutDate())) {
                throw new InvalidDateRangeException("Check-out date must be after check-in date");
            }
            if (!DateUtils.isWithinMaxDays(request.checkInDate(), request.checkOutDate(), hotelConfig.getMaxReservationDays())) {
                throw new InvalidDateRangeException(
                        "Reservation exceeds maximum allowed duration of " + hotelConfig.getMaxReservationDays() + " days");
            }

            List<Reservation> overlapping = reservationRepository.findOverlapping(
                    reservation.room.id, request.checkInDate(), request.checkOutDate());
            overlapping.removeIf(r -> r.id.equals(id));
            if (!overlapping.isEmpty()) {
                throw new RoomNotAvailableException(reservation.room.id.toString());
            }

            reservation.checkInDate = request.checkInDate();
            reservation.checkOutDate = request.checkOutDate();
            reservation.totalPrice = PriceCalculator.calculateTotalPrice(
                    reservation.room.pricePerNight, request.checkInDate(), request.checkOutDate());
        }

        if (request.numberOfGuests() != null) {
            reservation.numberOfGuests = request.numberOfGuests();
        }
        if (request.specialRequests() != null) {
            reservation.specialRequests = request.specialRequests();
        }

        reservationRepository.persist(reservation);
        cacheConfig.delete(Constants.CACHE_PREFIX_RESERVATION + id);
        LOG.infof("Reservation updated: id=%d, code=%s", id, reservation.confirmationCode);

        publishEvent(ReservationEvent.EventType.UPDATED, reservation);
        return reservation;
    }

    private void publishEvent(ReservationEvent.EventType eventType, Reservation reservation) {
        ReservationEvent event = new ReservationEvent(
                eventType,
                reservation.id,
                reservation.confirmationCode,
                reservation.guest.id,
                reservation.guest.email,
                reservation.room.id,
                reservation.checkInDate,
                reservation.checkOutDate,
                reservation.totalPrice,
                reservation.status.name(),
                LocalDateTime.now());
        eventPublisher.publish(event);
    }
}
