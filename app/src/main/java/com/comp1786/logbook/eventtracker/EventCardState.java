package com.comp1786.logbook.eventtracker;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Paint;
import android.widget.TextView;

import androidx.annotation.AttrRes;
import androidx.annotation.Nullable;

import com.google.android.material.color.MaterialColors;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Where an event is in time, or that it was cancelled, with the colors and text that show it.
 */
public enum EventCardState {

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
    // The only status in red, as the one that does not follow from the time.
    CANCELLED(com.google.android.material.R.attr.colorSurfaceContainerHighest,
            com.google.android.material.R.attr.colorOnSurfaceVariant,
            androidx.appcompat.R.attr.colorError);

    @AttrRes
    private final int datePanelColorAttr;
    @AttrRes
    private final int datePanelTextColorAttr;
    @AttrRes
    private final int statusColorAttr;

    EventCardState(@AttrRes int datePanelColorAttr, @AttrRes int datePanelTextColorAttr,
            @AttrRes int statusColorAttr) {
        this.datePanelColorAttr = datePanelColorAttr;
        this.datePanelTextColorAttr = datePanelTextColorAttr;
        this.statusColorAttr = statusColorAttr;
    }

    @AttrRes
    public int getDatePanelColorAttr() {
        return datePanelColorAttr;
    }

    @AttrRes
    public int getDatePanelTextColorAttr() {
        return datePanelTextColorAttr;
    }

    /** Returns when the next events start, or null while one is happening or none is ahead. */
    @Nullable
    public static LocalDateTime nextStart(List<Event> events, LocalDateTime now) {
        LocalDateTime nextStart = null;
        for (Event event : events) {
            EventCardState state = at(event, now, null);
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
    public static EventCardState at(Event event, LocalDateTime now,
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

    /** Shows the status phrase in its color, with a dot while the event is happening. */
    public void showStatus(TextView statusView, Event event, long daysAway) {
        statusView.setText(statusText(statusView.getContext(), event, daysAway));
        statusView.setTextColor(MaterialColors.getColor(statusView, statusColorAttr));
        statusView.setCompoundDrawablesRelativeWithIntrinsicBounds(
                this == LIVE ? R.drawable.ic_live : 0, 0, 0, 0);
    }

    /** Grays out a cancelled event and strikes through its name, or shows it as usual. */
    public void showCancelled(TextView nameView, TextView typeLabel, EventType type,
            TextView... timeViews) {
        boolean cancelled = this == CANCELLED;
        int textColor = MaterialColors.getColor(nameView, cancelled
                ? com.google.android.material.R.attr.colorOnSurfaceVariant
                : com.google.android.material.R.attr.colorOnSurface);
        nameView.setTextColor(textColor);
        for (TextView timeView : timeViews) {
            timeView.setTextColor(textColor);
        }
        int flags = nameView.getPaintFlags();
        nameView.setPaintFlags(cancelled
                ? flags | Paint.STRIKE_THRU_TEXT_FLAG : flags & ~Paint.STRIKE_THRU_TEXT_FLAG);
        typeLabel.setBackgroundTintList(ColorStateList.valueOf(cancelled
                ? MaterialColors.getColor(typeLabel,
                        com.google.android.material.R.attr.colorSurfaceContainerHighest)
                : typeLabel.getContext().getColor(type.getContainerColorRes())));
    }

    /** Describes the status with when the event is, such as "Upcoming in 6 days". */
    private String statusText(Context context, Event event, long daysAway) {
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
