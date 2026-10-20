package com.comp1786.logbook.eventtracker;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.core.widget.TextViewCompat;

import com.google.android.material.snackbar.Snackbar;

/**
 * Makes the app's messages: snackbars with an icon before their text.
 */
public final class IconSnackbar {

    private IconSnackbar() {
    }

    /** Returns a message at the bottom of the screen, with an icon before its text. */
    public static Snackbar make(View view, CharSequence message, @DrawableRes int iconRes,
            int duration) {
        // Without an anchor, a floating button on the screen moves up while the message shows.
        Snackbar snackbar = Snackbar.make(view, message, duration);
        TextView text =
                snackbar.getView().findViewById(com.google.android.material.R.id.snackbar_text);
        text.setCompoundDrawablesRelativeWithIntrinsicBounds(iconRes, 0, 0, 0);
        text.setCompoundDrawablePadding(
                view.getResources().getDimensionPixelSize(R.dimen.space_medium));
        TextViewCompat.setCompoundDrawableTintList(text, text.getTextColors());
        return snackbar;
    }
}
