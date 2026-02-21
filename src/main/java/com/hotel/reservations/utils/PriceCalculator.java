package com.hotel.reservations.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public final class PriceCalculator {

    private PriceCalculator() {
    }

    public static BigDecimal calculateTotalPrice(BigDecimal pricePerNight, LocalDate checkIn, LocalDate checkOut) {
        long nights = DateUtils.daysBetween(checkIn, checkOut);
        return pricePerNight.multiply(BigDecimal.valueOf(nights));
    }

    public static BigDecimal calculateRefundAmount(BigDecimal totalPrice, LocalDate checkIn, int hoursBeforePolicy) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime checkInDateTime = checkIn.atStartOfDay();
        long hoursUntilCheckIn = ChronoUnit.HOURS.between(now, checkInDateTime);

        if (hoursUntilCheckIn <= 0) {
            return BigDecimal.ZERO;
        }
        if (hoursUntilCheckIn > hoursBeforePolicy) {
            return totalPrice;
        }
        return totalPrice.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
    }
}
