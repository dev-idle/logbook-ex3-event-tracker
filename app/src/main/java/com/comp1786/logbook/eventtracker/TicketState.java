package com.comp1786.logbook.eventtracker;

import android.content.Context;
import android.content.res.Resources;

import androidx.annotation.AttrRes;
import androidx.annotation.Nullable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Where an event is in time, or that it was cancelled, with the colors and text that show it.
 */
public enum TicketState {

    LIVE(androidx.appcompat.R.attr.colorPrimary,
            com.google.android.material.R.attr.colorOnPrimary,
            androidx.appcompat.R.attr.colorPrimary),
    NEXT(androidx.appcompat.R.attr.colorPrimary,
            com.google.android.material.R.attr.colorOnPrimary,
            androidx.appcompat.R.attr.colorPrimary),
    UPCOMING(com.google.android.material.R.attr.colorPrimaryContainer,
            com.google.android.material.R.attr.colorOnPrimaryContainer,
            com.google.android.material.R.attr.colorOnSurfaceVariant),
    PAST(com.google.android.material.R.attr.colorSurfaceContainerHighest,
            com.google.android.material.R.attr.colorOnSurfaceVariant,
            com.google.android.material.R.attr.colorOnSurfaceVariant),
    // The only status in color, as the one that does not follow from the time.
    CANCELLED(com.google.android.material.R.attr.colorSurfaceContainerHighest,
            com.google.android.material.R.attr.colorOnSurfaceVariant,
            androidx.appcompat.R.attr.colorError);

    @AttrRes
    private final int stubColorAttr;
    @AttrRes
    private final int stubTextColorAttr;
    @AttrRes
    private final int statusColorAttr;

    TicketState(@AttrRes int stubColorAttr, @AttrRes int stubTextColorAttr,
            @AttrRes int statusColorAttr) {
        this.stubColorAttr = stubColorAttr;
        this.stubTextColorAttr = stubTextColorAttr;
        this.statusColorAttr = statusColorAttr;
    }

    @AttrRes
    public int getStubColorAttr() {
        return stubColorAttr;
    }

    @AttrRes
    public int getStubTextColorAttr() {
        return stubTextColorAttr;
    }

    @AttrRes
    public int getStatusColorAttr() {
        return statusColorAttr;
    }

    /** Returns when the next events start, or null while one is happening or none is ahead. */
    @Nullable
    public static LocalDateTime nextStart(List<Event> events, LocalDateTime now) {
        LocalDateTime nextStart = null;
        for (Event event : events) {
            TicketState state = at(event, now, null);
            if (state == LIVE) {
                return null;
            } else if (state == UPCOMING
                    && (nextStart == null || event.getStart().isBefore(nextStart))) {
                nextStart = event.getStart();
            }
        }
        return nextStart;
    }

    /** Returns the state of an event at the given time, given when the next events start. */
    public static TicketState at(Event event, LocalDateTime now,
            @Nullable LocalDateTime nextStart) {
        if (event.getStatus() == EventStatus.CANCELLED) {
            return CANCELLED;
        } else if (!event.getEnd().isAfter(now)) {
            return PAST;
        } else if (!event.getStart().isAfter(now)) {
            return LIVE;
        }
        return event.getStart().equals(nextStart) ? NEXT : UPCOMING;
    }

    /** Returns the days from today to the day the status counts to, negative once past. */
    public long daysAway(Event event, LocalDate today) {
        return ChronoUnit.DAYS.between(today, countdownDate(event));
    }

    /** Describes the status with when the event is, such as "Upcoming in 6 days". */
    public String statusText(Context context, Event event, long daysAway) {
        if (this == LIVE) {
            return context.getString(R.string.status_live);
        } else if (this == CANCELLED) {
            return context.getString(R.string.status_cancelled);
        }
        LocalDate date = countdownDate(event);
        LocalDate today = date.minusDays(daysAway);
        if (daysAway == 0) {
            return context.getString(this == PAST
                    ? R.string.status_completed_today : R.string.status_upcoming_today);
        } else if (daysAway == 1) {
            return context.getString(R.string.status_upcoming_tomorrow);
        } else if (daysAway == -1) {
            return context.getString(R.string.status_completed_yesterday);
        }
        Period period = daysAway > 0 ? Period.between(today, date) : Period.between(date, today);
        Resources res = context.getResources();
        String duration;
        if (period.getYears() > 0) {
            duration = res.getQuantityString(
                    R.plurals.duration_years, period.getYears(), period.getYears());
        } else if (period.getMonths() > 0) {
            duration = res.getQuantityString(
                    R.plurals.duration_months, period.getMonths(), period.getMonths());
        } else {
            duration = res.getQuantityString(
                    R.plurals.duration_days, period.getDays(), period.getDays());
        }
        return context.getString(daysAway > 0
                ? R.string.status_upcoming_in : R.string.status_completed_ago, duration);
    }

    /** Returns the day the status counts to: the end for past events, otherwise the start. */
    private LocalDate countdownDate(Event event) {
        return this == PAST ? event.getEnd().toLocalDate() : event.getDate();
    }
}
