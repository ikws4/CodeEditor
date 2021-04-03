package io.ikws4.codeeditor.core.span;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.NoCopySpan;
import android.text.TextPaint;
import android.text.style.CharacterStyle;
import android.text.style.MetricAffectingSpan;
import android.text.style.ReplacementSpan;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ReplacedSpan implements NoCopySpan {
    private final char[] mText;

    public ReplacedSpan(char[] text) {
        mText = text;
    }

    public char[] getText() {
        return mText;
    }
}
