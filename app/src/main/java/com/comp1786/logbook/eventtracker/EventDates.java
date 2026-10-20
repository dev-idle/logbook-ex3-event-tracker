package com.comp1786.logbook.eventtracker;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Locale;

/**
 * Reads and writes event dates in the DD/MM/YYYY format used by the brief.
 */
public final class EventDates {

    // The range of Android's DatePicker, which also catches typing mistakes such as 0026.
    public static final LocalDate MINIMUM = LocalDate.of(1900, 1, 1);
    public static final LocalDate MAXIMUM = LocalDate.of(2100, 12, 31);

    // STRICT rejects days that do not exist, such as 31/02/2026.
    private static final DateTimeFormatter INPUT = DateTimeFormatter.ofPattern("d/M/uuuu")
            .withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter OUTPUT = DateTimeFormatter.ofPattern("dd/MM/uuuu");
    // Day and month names are in English to match the rest of the app.
    private static final DateTimeFormatter SHORT_MONTH =
            DateTimeFormatter.ofPattern("MMM", Locale.UK);
    private static final DateTimeFormatter DAY_OF_MONTH =
            DateTimeFormatter.ofPattern("d", Locale.UK);
    private static final DateTimeFormatter SHORT_DAY_OF_WEEK =
            DateTimeFormatter.ofPattern("EEE", Locale.UK);
    private static final DateTimeFormatter LONG =
            DateTimeFormatter.ofPattern("EEEE d MMMM uuuu", Locale.UK);
    private static final DateTimeFormatter MONTH =
            DateTimeFormatter.ofPattern("MMMM uuuu", Locale.UK);

    private EventDates() {
    }

    /** Returns the date for text such as "5/3/2026", or null if it is not a real date. */
    public static LocalDate parse(String text) {
        try {
            return LocalDate.parse(text, INPUT);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /** Formats a date as DD/MM/YYYY, such as "05/03/2026". */
    public static String format(LocalDate date) {
        return date.format(OUTPUT);
    }

    /** Formats the month of a date, such as "Oct". */
    public static String formatShortMonth(LocalDate date) {
        return date.format(SHORT_MONTH);
    }

    /** Formats the day of the month, such as "24". */
    public static String formatDayOfMonth(LocalDate date) {
        return date.format(DAY_OF_MONTH);
    }

    /** Formats the day of the week, such as "Sat". */
    public static String formatShortDayOfWeek(LocalDate date) {
        return date.format(SHORT_DAY_OF_WEEK);
    }

    /** Formats a date in full, such as "Saturday 24 October 2026". */
    public static String formatLong(LocalDate date) {
        return date.format(LONG);
    }

    /** Formats a month with its year, such as "October 2026". */
    public static String formatMonth(YearMonth month) {
        return month.format(MONTH);
    }
}
