package com.comp1786.logbook.eventtracker;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.StateSet;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.color.MaterialColors;

/**
 * Draws an event card: a date panel and a body split by a dashed divider, notched at both ends.
 */
public class EventCardDrawable extends Drawable {

    private static final int[] SELECTED_STATE = {android.R.attr.state_selected};

    private final Paint bodyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint datePanelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dividerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint selectionPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path card = new Path();
    private final Path datePanel = new Path();
    private final float datePanelWidth;
    private final float cornerRadius;
    private final float notchRadius;
    private final float dashLength;
    private int alpha = 255;
    private boolean selected;

    public EventCardDrawable(Context context) {
        Resources res = context.getResources();
        datePanelWidth = res.getDimension(R.dimen.card_date_panel_width);
        cornerRadius = res.getDimension(R.dimen.card_corner_radius);
        notchRadius = res.getDimension(R.dimen.card_notch_radius);
        dashLength = res.getDimension(R.dimen.card_divider_dash);
        bodyPaint.setColor(context.getColor(R.color.card_body));
        dividerPaint.setColor(MaterialColors.getColor(context,
                com.google.android.material.R.attr.colorOutlineVariant, getClass().getName()));
        dividerPaint.setStyle(Paint.Style.STROKE);
        dividerPaint.setStrokeWidth(res.getDimension(R.dimen.card_divider_width));
        dividerPaint.setPathEffect(new DashPathEffect(new float[] {dashLength, dashLength}, 0));
        selectionPaint.setColor(MaterialColors.getColor(context,
                androidx.appcompat.R.attr.colorPrimary, getClass().getName()));
        selectionPaint.setStyle(Paint.Style.STROKE);
        // Twice as wide, as the half outside the card is clipped off.
        selectionPaint.setStrokeWidth(2 * res.getDimension(R.dimen.card_selection_width));
    }

    public void setDatePanelColor(@ColorInt int color) {
        datePanelPaint.setColor(color);
        datePanelPaint.setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    protected void onBoundsChange(@NonNull Rect bounds) {
        float dividerX = bounds.left + datePanelWidth;
        Path notches = new Path();
        notches.addCircle(dividerX, bounds.top, notchRadius, Path.Direction.CW);
        notches.addCircle(dividerX, bounds.bottom, notchRadius, Path.Direction.CW);
        card.reset();
        card.addRoundRect(new RectF(bounds), cornerRadius, cornerRadius, Path.Direction.CW);
        card.op(notches, Path.Op.DIFFERENCE);
        datePanel.reset();
        datePanel.addRect(bounds.left, bounds.top, dividerX, bounds.bottom, Path.Direction.CW);
        datePanel.op(card, Path.Op.INTERSECT);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        Rect bounds = getBounds();
        // Drawn on the body side of the date panel, stopping short of the notches.
        float x = bounds.left + datePanelWidth + dividerPaint.getStrokeWidth() / 2;
        float inset = notchRadius + dashLength;
        canvas.drawPath(card, bodyPaint);
        canvas.drawPath(datePanel, datePanelPaint);
        canvas.drawLine(x, bounds.top + inset, x, bounds.bottom - inset, dividerPaint);
        if (selected) {
            // Clipped to the card, so the outline follows the notches on the inside.
            canvas.save();
            canvas.clipPath(card);
            canvas.drawPath(card, selectionPaint);
            canvas.restore();
        }
    }

    @Override
    public boolean isStateful() {
        return true;
    }

    /** Outlines the card while its view is selected. */
    @Override
    protected boolean onStateChange(@NonNull int[] state) {
        boolean nowSelected = StateSet.stateSetMatches(SELECTED_STATE, state);
        if (nowSelected == selected) {
            return false;
        }
        selected = nowSelected;
        return true;
    }

    @Override
    public void setAlpha(int alpha) {
        this.alpha = alpha;
        bodyPaint.setAlpha(alpha);
        datePanelPaint.setAlpha(alpha);
        dividerPaint.setAlpha(alpha);
        selectionPaint.setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public int getAlpha() {
        return alpha;
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        bodyPaint.setColorFilter(colorFilter);
        datePanelPaint.setColorFilter(colorFilter);
        dividerPaint.setColorFilter(colorFilter);
        selectionPaint.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Override
    @SuppressWarnings("deprecation")
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
