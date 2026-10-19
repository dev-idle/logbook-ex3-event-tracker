package com.comp1786.logbook.eventtracker;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Checks the values entered for a new event, one field at a time.
 */
public final class EventValidator {

    /** Reasons an input is rejected. */
    public enum Error {
        EMPTY, WRONG_DATE_FORMAT, NOT_A_REAL_DATE, DATE_OUT_OF_RANGE, END_SAME_AS_START,
        COMPLETED_BEFORE_END, UPCOMING_AFTER_END
    }

    // One or two digits for the day and month and four for the year, such as "5/3/2026".
    private static final Pattern DATE = Pattern.compile("\\d{1,2}/\\d{1,2}/\\d{4}");

    private EventValidator() {
    }

    /** Validates a required text field: the name, location or organizer. */
    public static Result<String> validateText(String text) {
        String value = text.trim();
        return value.isEmpty() ? Result.invalid(Error.EMPTY) : Result.valid(value);
    }

    /** Validates a date typed as DD/MM/YYYY. */
    public static Result<LocalDate> validateDate(String text) {
        String value = text.trim();
        if (value.isEmpty()) {
            return Result.invalid(Error.EMPTY);
        }
        if (!DATE.matcher(value).matches()) {
            return Result.invalid(Error.WRONG_DATE_FORMAT);
        }
        LocalDate date = EventDates.parse(value);
        if (date == null) {
            return Result.invalid(Error.NOT_A_REAL_DATE);
        }
        if (date.isBefore(EventDates.MINIMUM) || date.isAfter(EventDates.MAXIMUM)) {
            return Result.invalid(Error.DATE_OUT_OF_RANGE);
        }
        return Result.valid(date);
    }

    /** Validates a time chosen from the list, which is empty until the user picks one. */
    public static Result<LocalTime> validateTime(String text) {
        LocalTime time = EventTimes.parse(text);
        return time == null ? Result.invalid(Error.EMPTY) : Result.valid(time);
    }

    /** Validates an end time, which may be on the next day but must differ from the start. */
    public static Result<LocalTime> validateEndTime(String text, LocalTime start) {
        Result<LocalTime> end = validateTime(text);
        if (end.isValid() && end.getValue().equals(start)) {
            return Result.invalid(Error.END_SAME_AS_START);
        }
        return end;
    }

    /** Validates the type chosen from the list, which is null until the user picks one. */
    public static Result<EventType> validateType(EventType type) {
        return type == null ? Result.invalid(Error.EMPTY) : Result.valid(type);
    }

    /** Validates a status: completed only after the end, if known, and upcoming only before. */
    public static Result<EventStatus> validateStatus(EventStatus status, LocalDateTime end,
            LocalDateTime now) {
        if (end != null) {
            boolean ended = !end.isAfter(now);
            if (status == EventStatus.COMPLETED && !ended) {
                return Result.invalid(Error.COMPLETED_BEFORE_END);
            }
            if (status == EventStatus.UPCOMING && ended) {
                return Result.invalid(Error.UPCOMING_AFTER_END);
            }
        }
        return Result.valid(status);
    }

    /** Returns true if an event with the same name, ignoring case, is already on that date. */
    public static boolean isDuplicate(String name, LocalDate date, List<Event> events) {
        for (Event event : events) {
            if (event.getDate().equals(date) && event.getName().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    /** Either a valid value or the reason it was rejected. */
    public static final class Result<T> {

        private final T value;
        private final Error error;

        private Result(T value, Error error) {
            this.value = value;
            this.error = error;
        }

        static <T> Result<T> valid(T value) {
            return new Result<>(value, null);
        }

        static <T> Result<T> invalid(Error error) {
            return new Result<>(null, error);
        }

        public boolean isValid() {
            return error == null;
        }

        /** Returns the trimmed or parsed value, or null when invalid. */
        public T getValue() {
            return value;
        }

        public Error getError() {
            return error;
        }
    }
}
