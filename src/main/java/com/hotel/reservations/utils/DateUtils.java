package com.hotel.reservations.utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public final class DateUtils {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private DateUtils() {
    }

    public static long daysBetween(LocalDate from, LocalDate to) {
        return ChronoUnit.DAYS.between(from, to);
    }

    public static boolean isDateRangeValid(LocalDate checkIn, LocalDate checkOut) {
        return checkIn != null && checkOut != null && checkOut.isAfter(checkIn);
    }

    public static boolean isWithinMaxDays(LocalDate checkIn, LocalDate checkOut, int maxDays) {
        return isDateRangeValid(checkIn, checkOut) && daysBetween(checkIn, checkOut) <= maxDays;
    }

    public static String formatDate(LocalDate date) {
        return date.format(FORMATTER);
    }

    public static boolean isDateInFuture(LocalDate date) {
        return date != null && date.isAfter(LocalDate.now());
    }
}
