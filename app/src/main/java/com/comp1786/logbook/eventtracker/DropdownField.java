package com.comp1786.logbook.eventtracker;

import android.content.Context;
import android.util.AttributeSet;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.ListAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.textfield.MaterialAutoCompleteTextView;

/**
 * A menu field that closes the keyboard and opens its list on the chosen item, or on a
 * preferred item while nothing is chosen.
 */
public class DropdownField extends MaterialAutoCompleteTextView {

    private String preferredItem = "";

    public DropdownField(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public void setPreferredItem(String item) {
        preferredItem = item;
    }

    @Override
    public void showDropDown() {
        WindowInsetsController insetsController = getWindowInsetsController();
        if (insetsController != null) {
            insetsController.hide(WindowInsets.Type.ime());
        }
        super.showDropDown();
        String target = length() > 0 ? getText().toString() : preferredItem;
        ListAdapter adapter = getAdapter();
        if (adapter == null) {
            return;
        }
        for (int i = 0; i < adapter.getCount(); i++) {
            if (target.equals(String.valueOf(adapter.getItem(i)))) {
                setListSelection(i);
                return;
            }
        }
    }
}
