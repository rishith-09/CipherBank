package com.paytrix.cipherbank.infrastructure.util;

import java.time.LocalDateTime;
import java.time.LocalTime;

public class DateTimeNormalizer {

    public static LocalDateTime normalizeToMidnight(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return LocalDateTime.of(dateTime.toLocalDate(), LocalTime.MIDNIGHT);
    }
}