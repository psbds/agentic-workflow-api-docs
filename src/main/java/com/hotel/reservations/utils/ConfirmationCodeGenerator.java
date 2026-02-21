package com.hotel.reservations.utils;

import java.util.UUID;

public final class ConfirmationCodeGenerator {

    private ConfirmationCodeGenerator() {
    }

    public static String generate() {
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return Constants.CONFIRMATION_CODE_PREFIX + uuid;
    }
}
