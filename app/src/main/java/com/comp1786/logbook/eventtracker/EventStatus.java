package com.comp1786.logbook.eventtracker;

import androidx.annotation.DrawableRes;
import androidx.annotation.PluralsRes;
import androidx.annotation.StringRes;

/**
 * Event statuses listed in the logbook brief.
 */
public enum EventStatus {

    UPCOMING(R.string.status_upcoming, R.plurals.event_count_upcoming, R.string.empty_upcoming,
            R.drawable.ic_event),
    COMPLETED(R.string.status_completed, R.plurals.event_count_completed,
            R.string.empty_completed, R.drawable.ic_event_available),
    CANCELLED(R.string.status_cancelled, R.plurals.event_count_cancelled,
            R.string.empty_cancelled, R.drawable.ic_event_busy);

    @StringRes
    private final int labelRes;
    @PluralsRes
    private final int countRes;
    @StringRes
    private final int emptyRes;
    @DrawableRes
    private final int iconRes;

    EventStatus(@StringRes int labelRes, @PluralsRes int countRes, @StringRes int emptyRes,
            @DrawableRes int iconRes) {
        this.labelRes = labelRes;
        this.countRes = countRes;
        this.emptyRes = emptyRes;
        this.iconRes = iconRes;
    }

    @StringRes
    public int getLabelRes() {
        return labelRes;
    }

    /** Returns the count shown while the list is filtered by this status. */
    @PluralsRes
    public int getCountRes() {
        return countRes;
    }

    /** Returns the title shown when no event has this status. */
    @StringRes
    public int getEmptyRes() {
        return emptyRes;
    }

    /** Returns the icon of its filter chip. */
    @DrawableRes
    public int getIconRes() {
        return iconRes;
    }
}
