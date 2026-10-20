package com.comp1786.logbook.eventtracker;

import androidx.annotation.Nullable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Checks the values entered for an event, one field at a time.
 */
public final class EventValidator {

    /** Reasons an input is rejected. */
    public enum Reason {
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
        return value.isEmpty() ? Result.invalid(Reason.EMPTY) : Result.valid(value);
    }

    /** Validates a date typed as DD/MM/YYYY. */
    public static Result<LocalDate> validateDate(String text) {
        String value = text.trim();
        if (value.isEmpty()) {
            return Result.invalid(Reason.EMPTY);
        }
        if (!DATE.matcher(value).matches()) {
            return Result.invalid(Reason.WRONG_DATE_FORMAT);
        }
        LocalDate date = EventDates.parse(value);
        if (date == null) {
            return Result.invalid(Reason.NOT_A_REAL_DATE);
        }
        if (date.isBefore(EventDates.MINIMUM) || date.isAfter(EventDates.MAXIMUM)) {
            return Result.invalid(Reason.DATE_OUT_OF_RANGE);
        }
        return Result.valid(date);
    }

    /** Validates a time chosen from the list, which is empty until the user picks one. */
    public static Result<LocalTime> validateTime(String text) {
        LocalTime time = EventTimes.parse(text);
        return time == null ? Result.invalid(Reason.EMPTY) : Result.valid(time);
    }

    /** Validates an end time, which may be on the next day but must differ from the start. */
    public static Result<LocalTime> validateEndTime(String text, @Nullable LocalTime start) {
        Result<LocalTime> end = validateTime(text);
        if (end.isValid() && end.getValue().equals(start)) {
            return Result.invalid(Reason.END_SAME_AS_START);
        }
        return end;
    }

    /** Validates the type chosen from the list, which is null until the user picks one. */
    public static Result<EventType> validateType(@Nullable EventType type) {
        return type == null ? Result.invalid(Reason.EMPTY) : Result.valid(type);
    }

    /** Validates a status: completed only after the end, if known, and upcoming only before. */
    public static Result<EventStatus> validateStatus(EventStatus status,
            @Nullable LocalDateTime end, LocalDateTime now) {
        if (end != null) {
            boolean ended = !end.isAfter(now);
            if (status == EventStatus.COMPLETED && !ended) {
                return Result.invalid(Reason.COMPLETED_BEFORE_END);
            }
            if (status == EventStatus.UPCOMING && ended) {
                return Result.invalid(Reason.UPCOMING_AFTER_END);
            }
        }
        return Result.valid(status);
    }

    /** Returns true if an event besides ignoredId has this name on that date, ignoring case. */
    public static boolean isDuplicate(String name, LocalDate date, long ignoredId,
            List<Event> events) {
        for (Event event : events) {
            if (event.getId() != ignoredId && event.getDate().equals(date)
                    && event.getName().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    /** Either a valid value or the reason it was rejected. */
    public static final class Result<T> {

        private final T value;
        private final Reason reason;

        private Result(T value, Reason reason) {
            this.value = value;
            this.reason = reason;
        }

        static <T> Result<T> valid(T value) {
            return new Result<>(value, null);
        }

        static <T> Result<T> invalid(Reason reason) {
            return new Result<>(null, reason);
        }

        public boolean isValid() {
            return reason == null;
        }

        /** Returns the trimmed or parsed value, or null when invalid. */
        @Nullable
        public T getValue() {
            return value;
        }

        @Nullable
        public Reason getReason() {
            return reason;
        }
    }
}
