package com.comp1786.logbook.eventtracker;

import androidx.annotation.ColorRes;
import androidx.annotation.StringRes;

/**
 * Event types listed in the logbook brief.
 */
public enum EventType {

    CONFERENCE(R.string.type_conference, R.color.type_conference_container),
    WORKSHOP(R.string.type_workshop, R.color.type_workshop_container),
    SEMINAR(R.string.type_seminar, R.color.type_seminar_container),
    SOCIAL(R.string.type_social, R.color.type_social_container),
    OTHER(R.string.type_other, R.color.type_other_container);

    @StringRes
    private final int labelRes;
    @ColorRes
    private final int containerColorRes;

    EventType(@StringRes int labelRes, @ColorRes int containerColorRes) {
        this.labelRes = labelRes;
        this.containerColorRes = containerColorRes;
    }

    @StringRes
    public int getLabelRes() {
        return labelRes;
    }

    @ColorRes
    public int getContainerColorRes() {
        return containerColorRes;
    }
}
