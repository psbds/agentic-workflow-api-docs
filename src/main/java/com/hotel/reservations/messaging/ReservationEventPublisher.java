package com.hotel.reservations.messaging;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ReservationEventPublisher {

    private static final Logger LOG = Logger.getLogger(ReservationEventPublisher.class);

    @Inject
    Event<ReservationEvent> reservationEvent;

    public void publish(ReservationEvent event) {
        LOG.infof("Publishing reservation event: type=%s, confirmationCode=%s, reservationId=%d",
                event.eventType(), event.confirmationCode(), event.reservationId());
        reservationEvent.fire(event);
    }

    public void publishAsync(ReservationEvent event) {
        LOG.infof("Publishing async reservation event: type=%s, confirmationCode=%s, reservationId=%d",
                event.eventType(), event.confirmationCode(), event.reservationId());
        reservationEvent.fireAsync(event);
    }
}
