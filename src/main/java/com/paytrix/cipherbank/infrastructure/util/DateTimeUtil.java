package com.paytrix.cipherbank.infrastructure.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for parsing dates and times from bank statements.
 * Ensures NO timezone conversion - dates are stored exactly as they appear in statements.
 * If time is not present, defaults to 00:00:00.
 */
public class DateTimeUtil {

    private static final Logger logger = LoggerFactory.getLogger(DateTimeUtil.class);

    /**
     * Common date formats used by Indian banks
     */
    private static final List<DateTimeFormatter> DATE_FORMATTERS = new ArrayList<>();
    private static final List<DateTimeFormatter> DATETIME_FORMATTERS = new ArrayList<>();

    static {
        // Date only formats (time will default to 00:00:00)
        DATE_FORMATTERS.add(DateTimeFormatter.ofPattern("dd-MMM-yyyy"));        // 21-Nov-2025
        DATE_FORMATTERS.add(DateTimeFormatter.ofPattern("dd/MM/yyyy"));         // 21/11/2025
        DATE_FORMATTERS.add(DateTimeFormatter.ofPattern("dd MMM yyyy"));        // 21 Nov 2025
        DATE_FORMATTERS.add(DateTimeFormatter.ofPattern("dd-MM-yyyy"));         // 21-11-2025
        DATE_FORMATTERS.add(DateTimeFormatter.ofPattern("yyyy-MM-dd"));         // 2025-11-21 (ISO)
        DATE_FORMATTERS.add(DateTimeFormatter.ofPattern("d-MMM-yyyy"));         // 1-Nov-2025 (single digit day)
        DATE_FORMATTERS.add(DateTimeFormatter.ofPattern("d/MM/yyyy"));          // 1/11/2025
        DATE_FORMATTERS.add(DateTimeFormatter.ofPattern("d MMM yyyy"));         // 1 Nov 2025

        // Date with time formats
        DATETIME_FORMATTERS.add(DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss"));    // 21-Nov-2025 14:30:00
        DATETIME_FORMATTERS.add(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));     // 21/11/2025 14:30:00
        DATETIME_FORMATTERS.add(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss"));    // 21 Nov 2025 14:30:00
        DATETIME_FORMATTERS.add(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"));     // 21-11-2025 14:30:00
        DATETIME_FORMATTERS.add(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));     // 2025-11-21 14:30:00
        DATETIME_FORMATTERS.add(DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm"));       // 21-Nov-2025 14:30
        DATETIME_FORMATTERS.add(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));        // 21/11/2025 14:30
        DATETIME_FORMATTERS.add(DateTimeFormatter.ofPattern("dd-MMM-yyyy'T'HH:mm:ss"));  // 21-Nov-2025T14:30:00
        DATETIME_FORMATTERS.add(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));   // 2025-11-21T14:30:00 (ISO)
    }

    /**
     * Parse a date/datetime string from a bank statement.
     * NO timezone conversion is applied.
     *
     * @param dateString The date string from the bank statement
     * @return LocalDateTime with the exact date from statement, time defaults to 00:00:00 if not present
     * @throws IllegalArgumentException if date cannot be parsed
     */
    public static LocalDateTime parseDateTime(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) {
            throw new IllegalArgumentException("Date string cannot be null or empty");
        }

        String trimmed = dateString.trim();

        // Try parsing as datetime first (with time component)
        for (DateTimeFormatter formatter : DATETIME_FORMATTERS) {
            try {
                LocalDateTime dt = LocalDateTime.parse(trimmed, formatter);
                logger.debug("Parsed datetime '{}' using format with time: {}", trimmed, dt);
                return dt;
            } catch (DateTimeParseException e) {
                // Continue to next format
            }
        }

        // Try parsing as date only (time will default to 00:00:00)
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                LocalDate date = LocalDate.parse(trimmed, formatter);
                LocalDateTime dt = LocalDateTime.of(date, LocalTime.MIDNIGHT); // 00:00:00
                logger.debug("Parsed date '{}' as LocalDateTime with 00:00:00 time: {}", trimmed, dt);
                return dt;
            } catch (DateTimeParseException e) {
                // Continue to next format
            }
        }

        // If all formats fail, throw exception
        throw new IllegalArgumentException(
                "Unable to parse date: '" + trimmed + "'. " +
                        "Supported formats: dd-MMM-yyyy, dd/MM/yyyy, dd MMM yyyy, yyyy-MM-dd, and variants with time."
        );
    }

    /**
     * Parse a date string and return as LocalDate (without time component)
     *
     * @param dateString The date string
     * @return LocalDate
     */
    public static LocalDate parseDate(String dateString) {
        return parseDateTime(dateString).toLocalDate();
    }

    /**
     * Format LocalDateTime for display (no timezone conversion)
     *
     * @param dateTime The LocalDateTime to format
     * @return Formatted string in dd-MMM-yyyy HH:mm:ss format
     */
    public static String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss"));
    }

    /**
     * Format LocalDateTime for display (date only)
     *
     * @param dateTime The LocalDateTime to format
     * @return Formatted string in dd-MMM-yyyy format
     */
    public static String formatDate(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(DateTimeFormatter.ofPattern("dd-MMM-yyyy"));
    }

    /**
     * Check if a datetime has time component (i.e., not midnight)
     *
     * @param dateTime The LocalDateTime to check
     * @return true if time is not 00:00:00, false otherwise
     */
    public static boolean hasTimeComponent(LocalDateTime dateTime) {
        if (dateTime == null) {
            return false;
        }
        return !dateTime.toLocalTime().equals(LocalTime.MIDNIGHT);
    }
}