package com.comp1786.logbook.eventtracker;

import androidx.annotation.StringRes;

/**
 * Event statuses listed in the logbook brief.
 */
public enum EventStatus {

    UPCOMING(R.string.status_upcoming),
    COMPLETED(R.string.status_completed),
    CANCELLED(R.string.status_cancelled);

    @StringRes
    private final int labelRes;

    EventStatus(@StringRes int labelRes) {
        this.labelRes = labelRes;
    }

    @StringRes
    public int getLabelRes() {
        return labelRes;
    }
}
