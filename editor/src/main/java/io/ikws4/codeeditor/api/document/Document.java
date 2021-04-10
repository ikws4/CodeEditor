package io.ikws4.codeeditor.api.document;

import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import io.ikws4.codeeditor.api.document.markup.Markup;

public class Document extends SpannableStringBuilder {
    private final List<Markup> mMarkups;
    private boolean mUpdating = false;
    private int mStart;
    private int mEnd;

    public Document(CharSequence source) {
        super(source);
        mMarkups = new ArrayList<>();
    }

    public  void setMarkupSource(List<Markup> spans) {
        mMarkups.clear();
        mMarkups.addAll(spans);
    }

    public void setMarkup(Markup span) {
        setSpan(span, span.getStart(), span.getEnd(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    /**
     * Notify the visible area was changed, in order to refresh the markups
     */
    public void notifyVisibleRangeChanged(int start, int end, boolean updateAll) {
        if (mUpdating) return;

        mUpdating = true;
        if (updateAll) {
            removeMarkups(mStart, mEnd);
            addMarkups(start, end);
        } else {
            if (start >= mStart) {
                removeMarkups(mStart, start);
                addMarkups(mEnd, end);
            } else {
                addMarkups(start, mStart);
                removeMarkups(end, mEnd);
            }
        }
        mStart = start;
        mEnd = end;
        mUpdating = false;
    }

    public void notifyTextChanged(int start, int offset) {
        if (mUpdating) return;

        mUpdating = true;
        shiftMarkups(start, offset);
        mUpdating = false;
    }

    /**
     * Shift spans by given start and offset.
     */
    private void shiftMarkups(int start, int offset) {
        for (int i = markupIndexAt(start); i < mMarkups.size(); i++) {
            mMarkups.get(i).shift(offset);
        }
    }

    /**
     * Remove span by given range (start, end)
     */
    private void removeMarkups(int start, int end) {
        Markup[] spans = getSpans(start, end, Markup.class);
        for (Markup span : spans) {
            removeSpan(span);
        }
    }

    /**
     * Add span by given range (start, end)
     */
    private void addMarkups(int start, int end) {
        int startIndex = markupIndexAt(start);
        int endIndex = Math.min(mMarkups.size() - 1, markupIndexAt(end));
        for (int i = startIndex; i <= endIndex; i++) {
            Markup span = mMarkups.get(i);
            setMarkup(span);
        }
    }

    private int markupIndexAt(int start) {
        int l = 0, r = mMarkups.size(), m;
        while (l < r) {
            m = l + (r - l) / 2;
            if (mMarkups.get(m).getStart() < start) {
                l = m + 1;
            } else {
                r = m;
            }
        }
        return l;
    }

    public static class Factory extends Editable.Factory {
        private static final Document.Factory sInstance = new Document.Factory();

        public static Document.Factory getInstance() {
            return sInstance;
        }

        @Override
        public Editable newEditable(CharSequence source) {
            return new Document(source);
        }
    }
}
