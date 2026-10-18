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

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.color.MaterialColors;

/**
 * Draws an event ticket: a date stub and a body split by a perforation, notched at both ends.
 */
public class TicketDrawable extends Drawable {

    private final Paint bodyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stubPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint perforationPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path ticket = new Path();
    private final Path stub = new Path();
    private final float stubWidth;
    private final float cornerRadius;
    private final float notchRadius;
    private final float dashLength;
    private int alpha = 255;

    public TicketDrawable(Context context) {
        Resources res = context.getResources();
        stubWidth = res.getDimension(R.dimen.ticket_stub_width);
        cornerRadius = res.getDimension(R.dimen.ticket_corner_radius);
        notchRadius = res.getDimension(R.dimen.ticket_notch_radius);
        dashLength = res.getDimension(R.dimen.ticket_perforation_dash);
        bodyPaint.setColor(context.getColor(R.color.ticket_body));
        perforationPaint.setColor(MaterialColors.getColor(context,
                com.google.android.material.R.attr.colorOutlineVariant, getClass().getName()));
        perforationPaint.setStyle(Paint.Style.STROKE);
        perforationPaint.setStrokeWidth(res.getDimension(R.dimen.ticket_perforation_width));
        perforationPaint.setPathEffect(new DashPathEffect(new float[] {dashLength, dashLength}, 0));
    }

    public void setStubColor(@ColorInt int color) {
        stubPaint.setColor(color);
        stubPaint.setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    protected void onBoundsChange(@NonNull Rect bounds) {
        float perforationX = bounds.left + stubWidth;
        Path notches = new Path();
        notches.addCircle(perforationX, bounds.top, notchRadius, Path.Direction.CW);
        notches.addCircle(perforationX, bounds.bottom, notchRadius, Path.Direction.CW);
        ticket.reset();
        ticket.addRoundRect(new RectF(bounds), cornerRadius, cornerRadius, Path.Direction.CW);
        ticket.op(notches, Path.Op.DIFFERENCE);
        stub.reset();
        stub.addRect(bounds.left, bounds.top, perforationX, bounds.bottom, Path.Direction.CW);
        stub.op(ticket, Path.Op.INTERSECT);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        Rect bounds = getBounds();
        // Drawn on the body side of the stub, stopping short of the notches.
        float x = bounds.left + stubWidth + perforationPaint.getStrokeWidth() / 2;
        float inset = notchRadius + dashLength;
        canvas.drawPath(ticket, bodyPaint);
        canvas.drawPath(stub, stubPaint);
        canvas.drawLine(x, bounds.top + inset, x, bounds.bottom - inset, perforationPaint);
    }

    @Override
    public void setAlpha(int alpha) {
        this.alpha = alpha;
        bodyPaint.setAlpha(alpha);
        stubPaint.setAlpha(alpha);
        perforationPaint.setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public int getAlpha() {
        return alpha;
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        bodyPaint.setColorFilter(colorFilter);
        stubPaint.setColorFilter(colorFilter);
        perforationPaint.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Override
    @SuppressWarnings("deprecation")
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
