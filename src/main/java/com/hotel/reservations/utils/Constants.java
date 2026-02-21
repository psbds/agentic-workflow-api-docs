package com.hotel.reservations.utils;

public final class Constants {

    private Constants() {
    }

    public static final String CACHE_PREFIX_HOTEL = "hotel:";
    public static final String CACHE_PREFIX_ROOM = "room:";
    public static final String CACHE_PREFIX_RESERVATION = "reservation:";
    public static final String CACHE_PREFIX_AVAILABILITY = "availability:";

    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;

    public static final String CONFIRMATION_CODE_PREFIX = "HTL-";
}
