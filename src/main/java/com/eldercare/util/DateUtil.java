package com.eldercare.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * DateUtil provides standard date and time formatters and parsing methods
 * according to project specifications:
 * - Dates in "dd-MM-yyyy"
 * - Times in "HH:mm"
 * - Date-times in "dd-MM-yyyy HH:mm"
 */
public final class DateUtil {

    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    private DateUtil() {
        // Utility class; prevent instantiation
    }

    /**
     * Parses a date string in "dd-MM-yyyy" format.
     *
     * @param text date string
     * @return LocalDate
     * @throws IllegalArgumentException if format is invalid
     */
    public static LocalDate parseDate(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Date cannot be empty.");
        }
        try {
            return LocalDate.parse(text.trim(), DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format. Expected dd-MM-yyyy (e.g. 15-08-2026).");
        }
    }

    /**
     * Formats a LocalDate to "dd-MM-yyyy" string.
     *
     * @param date LocalDate
     * @return formatted date string
     */
    public static String formatDate(LocalDate date) {
        return (date != null) ? date.format(DATE_FORMATTER) : "";
    }

    /**
     * Parses a time string in "HH:mm" format.
     *
     * @param text time string
     * @return LocalTime
     * @throws IllegalArgumentException if format is invalid
     */
    public static LocalTime parseTime(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Time cannot be empty.");
        }
        try {
            return LocalTime.parse(text.trim(), TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid time format. Expected HH:mm (24-hour, e.g. 09:30).");
        }
    }

    /**
     * Formats a LocalTime to "HH:mm" string.
     *
     * @param time LocalTime
     * @return formatted time string
     */
    public static String formatTime(LocalTime time) {
        return (time != null) ? time.format(TIME_FORMATTER) : "";
    }

    /**
     * Parses a date-time string in "dd-MM-yyyy HH:mm" format.
     *
     * @param text date-time string
     * @return LocalDateTime
     * @throws IllegalArgumentException if format is invalid
     */
    public static LocalDateTime parseDateTime(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Date and time cannot be empty.");
        }
        try {
            return LocalDateTime.parse(text.trim(), DATE_TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date-time format. Expected dd-MM-yyyy HH:mm (e.g. 15-08-2026 14:30).");
        }
    }

    /**
     * Formats a LocalDateTime to "dd-MM-yyyy HH:mm" string.
     *
     * @param dateTime LocalDateTime
     * @return formatted date-time string
     */
    public static String formatDateTime(LocalDateTime dateTime) {
        return (dateTime != null) ? dateTime.format(DATE_TIME_FORMATTER) : "";
    }
}
