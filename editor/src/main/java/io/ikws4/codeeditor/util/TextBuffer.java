package io.ikws4.codeeditor.util;

import android.text.Editable;
import android.text.Spannable;

import java.util.List;

import io.ikws4.codeeditor.api.language.ExtendedSpan;

/**
 * A helper class for {@link Editable}
 */
public class TextBuffer {
    public static void setText(Editable content, CharSequence text) {
        content.replace(0, content.length(), text);
    }

    public static void setSpan(Editable content, ExtendedSpan span) {
        content.setSpan(span, span.getStart(), span.getEnd(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    /**
     * Clean span by given range (start, end)
     */
    public static void cleanSpans(Editable content, int start, int end) {
        ExtendedSpan[] spans = content.getSpans(start, end , ExtendedSpan.class);
        for (ExtendedSpan span : spans) {
            content.removeSpan(span);
        }
    }

    /**
     * Add span by given range (start, end)
     */
    public static void addSpans(Editable content, List<ExtendedSpan> source, int start, int end) {
        int startIndex = getSpanIndexAt(source, start);
        int endIndex= getSpanIndexAt(source, end);
        for (int i = startIndex; i < endIndex; i++) {
            ExtendedSpan span = source.get(i);
            TextBuffer.setSpan(content, span);
        }
    }

    /**
     * Shift spans by given start and offset.
     */
    public static void shiftSpans(List<ExtendedSpan> source, int start, int offset) {
        for (int i = getSpanIndexAt(source, start); i < source.size(); i++) {
            source.get(i).shift(offset);
        }
    }

    private static int getSpanIndexAt(List<ExtendedSpan> source, int start) {
        int l = 0, r = source.size(), m;
        while (l < r) {
            m = l + (r - l) / 2;
            if (source.get(m).getStart() < start) {
                l = m + 1;
            } else {
                r = m;
            }
        }
        return l;
    }
}
