package net.gsantner.markor.util;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * List-row background in the style of ColorNote:
 * a light tinted fill across the whole row plus a solid, narrow strip on the left edge.
 * Works on all API levels (no LayerDrawable gravity/width APIs needed).
 */
public class ColorNoteRowDrawable extends Drawable {
    private final Paint _fill = new Paint();
    private final Paint _strip = new Paint();
    private final int _stripWidthPx;

    public ColorNoteRowDrawable(final int fillColor, final int stripColor, final int stripWidthPx) {
        _fill.setColor(fillColor);
        _strip.setColor(stripColor);
        _stripWidthPx = stripWidthPx;
    }

    @Override
    public void draw(@NonNull final Canvas canvas) {
        final Rect b = getBounds();
        canvas.drawRect(b, _fill);
        canvas.drawRect(b.left, b.top, b.left + _stripWidthPx, b.bottom, _strip);
    }

    @Override
    public void setAlpha(final int alpha) {
        _fill.setAlpha(alpha);
        _strip.setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public void setColorFilter(@Nullable final ColorFilter colorFilter) {
        _fill.setColorFilter(colorFilter);
        _strip.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.OPAQUE;
    }
}
