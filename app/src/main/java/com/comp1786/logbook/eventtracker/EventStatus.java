package com.comp1786.logbook.eventtracker;

import androidx.annotation.PluralsRes;
import androidx.annotation.StringRes;

/**
 * Event statuses listed in the logbook brief.
 */
public enum EventStatus {

    UPCOMING(R.string.status_upcoming, R.plurals.event_count_upcoming, R.string.empty_upcoming),
    COMPLETED(R.string.status_completed, R.plurals.event_count_completed,
            R.string.empty_completed),
    CANCELLED(R.string.status_cancelled, R.plurals.event_count_cancelled,
            R.string.empty_cancelled);

    @StringRes
    private final int labelRes;
    @PluralsRes
    private final int countRes;
    @StringRes
    private final int emptyRes;

    EventStatus(@StringRes int labelRes, @PluralsRes int countRes, @StringRes int emptyRes) {
        this.labelRes = labelRes;
        this.countRes = countRes;
        this.emptyRes = emptyRes;
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
}
