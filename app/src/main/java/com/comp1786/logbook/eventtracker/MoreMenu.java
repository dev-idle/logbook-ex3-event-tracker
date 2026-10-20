package com.comp1786.logbook.eventtracker;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.LayoutRes;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.ListPopupWindow;

/**
 * Opens a menu of one item under its button, only as wide as the item, unlike the overflow menu.
 */
public final class MoreMenu {

    private MoreMenu() {
    }

    /** Shows the item under the anchor and returns the menu, to close along with the screen. */
    public static ListPopupWindow show(Context context, View anchor, @LayoutRes int itemLayout,
            @StringRes int titleRes, Runnable onClick) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, itemLayout,
                new String[] {context.getString(titleRes)});
        View item = adapter.getView(0, null, (ViewGroup) anchor.getParent());
        // Measured before it is attached, so it needs a direction to place its start icon.
        item.setLayoutDirection(anchor.getLayoutDirection());
        item.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        ListPopupWindow menu = new ListPopupWindow(
                context, null, androidx.appcompat.R.attr.actionOverflowMenuStyle);
        menu.setAdapter(adapter);
        menu.setAnchorView(anchor);
        menu.setDropDownGravity(Gravity.END);
        menu.setContentWidth(item.getMeasuredWidth());
        menu.setModal(true);
        menu.setOnItemClickListener((parent, view, position, id) -> {
            menu.dismiss();
            onClick.run();
        });
        menu.show();
        return menu;
    }
}
