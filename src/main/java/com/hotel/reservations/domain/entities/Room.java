package com.hotel.reservations.domain.entities;

import com.hotel.reservations.domain.enums.RoomType;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "rooms")
public class Room extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "room_number", nullable = false)
    public String roomNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "room_type", nullable = false)
    public RoomType roomType;

    @Column(name = "price_per_night", nullable = false, precision = 10, scale = 2)
    public BigDecimal pricePerNight;

    @Column(name = "max_occupancy")
    public int maxOccupancy;

    public String description;

    @Column(name = "is_available")
    public boolean isAvailable;

    @Column(name = "floor_number")
    public int floorNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id", nullable = false)
    public Hotel hotel;

    @Column(name = "created_at", updatable = false)
    public LocalDateTime createdAt;

    @Column(name = "updated_at")
    public LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
