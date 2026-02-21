package com.hotel.reservations.domain.entities;

import com.hotel.reservations.domain.enums.PaymentStatus;
import com.hotel.reservations.domain.enums.ReservationStatus;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "reservations")
public class Reservation extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "confirmation_code", nullable = false, unique = true)
    public String confirmationCode;

    @Column(name = "check_in_date", nullable = false)
    public LocalDate checkInDate;

    @Column(name = "check_out_date", nullable = false)
    public LocalDate checkOutDate;

    @Column(name = "number_of_guests")
    public int numberOfGuests;

    @Column(name = "total_price", precision = 10, scale = 2)
    public BigDecimal totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public ReservationStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    public PaymentStatus paymentStatus;

    @Column(name = "special_requests")
    public String specialRequests;

    @Column(name = "weather_checked")
    public boolean weatherChecked;

    @Column(name = "weather_summary")
    public String weatherSummary;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guest_id", nullable = false)
    public Guest guest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    public Room room;

    @Column(name = "created_at", updatable = false)
    public LocalDateTime createdAt;

    @Column(name = "updated_at")
    public LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (confirmationCode == null) {
            confirmationCode = "RES-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
