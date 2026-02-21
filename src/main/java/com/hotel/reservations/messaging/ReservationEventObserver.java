package com.hotel.reservations.messaging;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ReservationEventObserver {

    private static final Logger LOG = Logger.getLogger(ReservationEventObserver.class);

    public void onReservationEvent(@Observes ReservationEvent event) {
        LOG.infof("Received reservation event: type=%s, confirmationCode=%s, guestId=%d, checkIn=%s, checkOut=%s",
                event.eventType(), event.confirmationCode(), event.guestId(),
                event.checkInDate(), event.checkOutDate());

        switch (event.eventType()) {
            case CREATED -> LOG.infof("New reservation created: %s for guest %d, room %d",
                    event.confirmationCode(), event.guestId(), event.roomId());
            case CONFIRMED -> LOG.infof("Reservation confirmed: %s", event.confirmationCode());
            case CANCELLED -> LOG.infof("Reservation cancelled: %s", event.confirmationCode());
            case CHECKED_IN -> LOG.infof("Guest checked in: %s", event.confirmationCode());
            case CHECKED_OUT -> LOG.infof("Guest checked out: %s", event.confirmationCode());
            case UPDATED -> LOG.infof("Reservation updated: %s", event.confirmationCode());
            case EXPIRED -> LOG.infof("Reservation expired: %s", event.confirmationCode());
        }

        // Placeholder for future integrations:
        // - Email notifications
        // - Analytics tracking
        // - External system synchronization
    }
}
