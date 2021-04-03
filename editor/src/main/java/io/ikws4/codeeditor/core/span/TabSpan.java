package io.ikws4.codeeditor.core.span;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.Layout;
import android.text.TextPaint;
import android.text.style.LeadingMarginSpan;
import android.util.Log;

import androidx.annotation.ColorInt;

public class TabSpan extends ReplacedSpan implements LeadingMarginSpan {
    private final String mTab;
    private final int mColor;

    public TabSpan(String tab, @ColorInt int color) {
        super(new char[0]);
        mTab = tab;
        mColor = color;
    }

    @Override
    public int getLeadingMargin(boolean first) {
        return 0;
    }

    private static final String TAG = "TabSpan";
    @Override
    public void drawLeadingMargin(Canvas c, Paint p, int x, int dir, int top, int baseline, int bottom, CharSequence text, int start, int end, boolean first, Layout layout) {
        if (mTab.length() <= 0) return;
        float charWidth = p.measureText(String.valueOf(mTab.charAt(0)));

        int centerY = top + (bottom - top) / 2;
        float[] points = new float[2 * mTab.length()];
        for (int i = 0; i < points.length; i += 2) {
            points[i] = charWidth * (i + 1) / 2;
            points[i + 1] = centerY;
        }

        TextPaint paint = new TextPaint(p);
        paint.setColor(mColor);
        paint.setAntiAlias(true);
        paint.setStrokeWidth(4);
        paint.setStrokeCap(Paint.Cap.ROUND);
        c.drawPoints(points, paint);
    }
}
